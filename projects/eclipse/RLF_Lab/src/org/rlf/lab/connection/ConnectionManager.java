/**
 * Hilo de comunicación con el cliente.
 */
package org.rlf.lab.connection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;

import org.rlf.lab.LabContext;
import org.rlf.lab.connection.user.ExecutionRequest;
import org.rlf.lab.connection.user.User;
import org.rlf.log.RLF_Log;
import org.rlf.net.RLF_NetHelper;
import org.rlf.net.message.RLF_NetMessage;
import org.rlf.net.message.RLF_NetMessageID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * Asistente para la comunicación con el cliente. Acepta peticiones de clientes
 * válidados por el proveedor. Además, envía los mensajes indicados por el
 * gestor de ejecución.
 * 
 * @author Carlos A. Rodriguez Mecha
 * @version 0.1
 */
public class ConnectionManager extends Thread {

	// Constantes:
	/** Tiempo máximo de espera del hilo. */
	public final static int DELAY = 500;

	// Atributos:
	/** Lista con mensajes a enviar a los clientes en orden. */
	private LinkedList<RLF_NetMessage> msgQueue;
	/**
	 * Lista con los usuarios que deben recibir el mensaje. Corresponde con las
	 * posiciones de los mensajes almacenados en msqQueue. Puede que haya
	 * usuarios de los que no se acepten más peticiones.
	 */
	private LinkedList<User> userQueue;
	/** Lista de usuarios que pueden realizar peticiones de ejecución. */
	private HashMap<String, User> activeUsers;
	/** Contexto. */
	private LabContext context;
	/** Indica que se ha producido una petición de parada total. */
	private boolean stopRequest;

	// Constructor:
	/**
	 * Constructor del asistente.
	 * 
	 * @param context
	 *            Contexto.
	 */
	public ConnectionManager(LabContext context) {
		super("ConnectionManager");
		this.context = context;
		this.msgQueue = new LinkedList<RLF_NetMessage>();
		this.userQueue = new LinkedList<User>();
		this.activeUsers = new HashMap<String, User>();
		this.stopRequest = false;
	}

	// Métodos varios:
	/**
	 * Realiza una parada completa de las conexiones. Descarta los mensajes en
	 * cola y envía a cada usuario conectado el mensaje de parada. Termina la
	 * ejecución actual cerrando los sockets de conexión. Es usado para paradas
	 * de emergencia y desactivaciones. Si el hilo no está ejecutando o ya se ha
	 * realizado la petición de llamada, no tiene efecto.
	 */
	public synchronized void stopConnections() {

		if (!this.isAlive() || this.stopRequest)
			return;

		this.msgQueue.clear();
		this.userQueue.clear();

		for (User user : this.activeUsers.values()) {

			// 1. Se notifica al usuario.
			RLF_NetMessage msg = new RLF_NetMessage();
			user.stopTimer();
			msg.setId(RLF_NetMessageID.STOPLAB);
			this.msgQueue.add(msg);
			this.userQueue.add(user);

			// 2. Se cierra su socket de peticiones.
			try {
				user.getRequestSocket().close();
				user.setRequestSocket(null);
			} catch (IOException e) {
			}
		}

		// 3. Se eliminan todos los usuarios que pueden realizar peticiones.
		this.activeUsers.clear();

		this.stopRequest = true;
	}

	// Métodos de conexión:
	/**
	 * Añade un token a la lista de usuarios válidos. Sólo se ejecuta si el hilo
	 * está en ejecución, ni en parada de emergencia ni en parada segura.
	 * 
	 * @param token
	 *            Tocken de usuario.
	 * @param timeout
	 *            Tiempo máximo de valided en minutos.
	 * @param tools
	 *            Lista de herramientas que puede usar el usuario.
	 */
	public synchronized void addUser(String token, int timeout,
			LinkedList<Integer> tools) {

		if (!this.isAlive() || this.stopRequest)
			return;
		if (this.activeUsers.containsKey(token))
			return;

		Timer timer = new Timer();
		User user = new User(token, timeout, tools, timer);
		timer.scheduleAtFixedRate(new TimeoutTask(this.context, user),
				timeout * 60 * 1000, timeout * 60 * 1000);

		this.activeUsers.put(token, user);

	}

	/**
	 * Elimina a un usuario del laboratorio. Esto se produce por desconexión
	 * voluntaria. Al usuario no se le notifica nada. Si el hilo tiene una
	 * petición de parada o no está en ejecución, no tiene efecto.
	 * 
	 * @param token
	 *            Token de uso del usuario a eliminar.
	 */
	public synchronized void removeUser(String token) {

		if (!this.isAlive() || this.stopRequest)
			return;

		User user = this.activeUsers.get(token);
		if (user == null)
			return;

		// 1. Se para el cronómetro y cierra sus sockets.
		user.stopTimer();
		try {
			user.getNotificationSocket().close();
			user.setNotificationSocket(null);
			user.getRequestSocket().close();
			user.setRequestSocket(null);
		} catch (Exception e) {
		}

		// 2. Se le elimina de la lista de usuarios válidos.
		this.activeUsers.remove(token);

		// 3. Se indica al asistente de ejecución que pare sus rutinas.
		this.context.getRuntimeManager().disconnectUser(user);

		// 4. Se eliminan los mensajes que estaban por enviarse.
		while (this.userQueue.contains(user)) {
			int index = this.userQueue.indexOf(user);
			this.msgQueue.remove(index);
			this.userQueue.remove(index);
		}

	}

	/**
	 * Indica al usuario que su tiempo ha excedido. El mensaje se añade a la
	 * lista de mensajes pendientes. Si hay una petición de parada o el hilo no
	 * está en ejecución no hará nada. También lo elimina de la lista de
	 * usuarios válidos.
	 * 
	 * @param user
	 *            Usuario.
	 */
	public synchronized void sendTimeoutMsg(User user) {

		if (!this.isAlive() || this.stopRequest)
			return;

		RLF_NetMessage msg = new RLF_NetMessage();
		msg.setId(RLF_NetMessageID.TIMEOUT);

		// 1. Se le elimina de la lista de usuarios válidos.
		this.activeUsers.remove(user.getToken());

		// 2. Se cierra el socket de peticiones.
		try {
			user.getRequestSocket().close();
		} catch (Exception e) {
		}

		// 3. Se le notifica.
		this.msgQueue.add(msg);
		this.userQueue.add(user);

	}

	/**
	 * Envía al usuario el resultado de su ejecución. Sólo se utiliza para
	 * herramientas que no son de datos. Si hay una petición de parada, el hilo
	 * no está en ejecución o el usuario no tiene un estado válido (fuera de
	 * tiempo o desconexión) no hará nada.
	 * 
	 * @param ticket
	 *            Ticket asociado a la ejecución.
	 * @param ex
	 *            Petición de ejecución.
	 */
	public void sendResultMsg(String ticket, ExecutionRequest ex) {

		if (!this.isAlive() || this.stopRequest)
			return;

		if (ex.getUser() != null
				&& ex.getUser().getNotificationSocket() == null)
			return;

		RLF_NetMessage msg = new RLF_NetMessage();
		HashMap<String, String> attributes = new HashMap<String, String>();

		// 1. Finalización correcta.
		if (ex.getStatus() == ExecutionRequest.RequestStatus.FINISH) {
			msg.setId(RLF_NetMessageID.EXEC_FINISH);
			// 2. Finalización con errores.
		} else
			msg.setId(RLF_NetMessageID.EXEC_ERROR);

		attributes.put("ticket", ticket);
		if (ex.getChanges() != null) {
			for (Entry<String, String> e : ex.getChanges().entrySet()) {
				attributes.put(e.getKey(), e.getValue());
			}

			msg.setAttributes(attributes);
		}

		synchronized (this) {
			this.msgQueue.add(msg);
			this.userQueue.add(ex.getUser());
		}

	}

	/**
	 * Envía al usuario el ticket de la acción que se va a ejecutar. No notifica
	 * las acciones reseteadoras. Si hay una petición de parada o el hilo no
	 * está en ejecución no hará nada.
	 * 
	 * @param ex
	 *            Petición de ejecución.
	 * @param ticket
	 *            Ticket de la acción.
	 * @param portIn
	 *            Puerto de entrada.
	 * @param portOut
	 *            Puerto de salida.
	 */
	public void sendTicket(ExecutionRequest ex, String ticket, int portIn,
			int portOut) {

		if (!this.isAlive() || this.stopRequest)
			return;

		HashMap<String, User> users = new HashMap<String, User>();
		users.put(ex.getUser().getToken(), ex.getUser());
		for (User u : ex.getUsers()) {
			users.put(u.getToken(), u);
		}

		for (User u : users.values()) {

			RLF_NetMessage msg = new RLF_NetMessage();
			HashMap<String, String> attributes = new HashMap<String, String>();
			attributes.put("tool", new Integer(ex.getIdTool()).toString());
			attributes.put("action", ex.getActionName());

			if (ex.getStatus() != ExecutionRequest.RequestStatus.ERROR) {

				attributes.put("ticket", ticket);
				if (portIn > 0)
					attributes.put("portIn", new Integer(portIn).toString());
				if (portOut > 0)
					attributes.put("portOut", new Integer(portOut).toString());
				msg.setId(RLF_NetMessageID.EXEC);

			} else
				msg.setId(RLF_NetMessageID.EXEC_ERROR);

			msg.setAttributes(attributes);

			synchronized (this) {
				this.msgQueue.add(msg);
				this.userQueue.add(u);
			}

		}

	}

	/**
	 * Verifica el formato de un mensaje enviado por un cliente con una petición
	 * de ejecución. Después crea la petición si todo ha ido bien.
	 * 
	 * @param token
	 *            Token de uso.
	 * @param msg
	 *            Mensaje enviado por el cliente.
	 * @return Petición construida. Null si el usuario no tiene acceso.
	 * @throws RuntimeException
	 *             El formato del mensaje es inválido.
	 */
	protected ExecutionRequest createRequest(String token, RLF_NetMessage msg)
			throws RuntimeException {

		int tool;
		String action, pname, pvalue;

		HashMap<String, String> attributes, changes = new HashMap<String, String>();
		JsonParser parser = new JsonParser();
		JsonObject parameters;

		// 1. Petición
		if (msg.getId() != RLF_NetMessageID.EXEC)
			throw new RuntimeException();

		// 2. Autentificación del usuario.
		User user = this.activeUsers.get(token);

		try {

			attributes = msg.getAttributes();

			tool = Integer.parseInt(attributes.get("tool"));
			if (!this.context.getToolManager().getTools().containsKey(tool))
				throw new RuntimeException();

			if (!user.canUse(tool))
				throw null;

			action = attributes.get("action");
			if (action == null)
				throw new RuntimeException();

			parameters = parser.parse(attributes.get("parameters"))
					.getAsJsonObject();
			for (Entry<String, JsonElement> e : parameters.entrySet()) {
				pname = e.getKey();
				pvalue = e.getValue().getAsString();
				changes.put(pname, pvalue);
			}

		} catch (NullPointerException e) {
			throw new RuntimeException();
		} catch (NumberFormatException e) {
			throw new RuntimeException();
		} catch (JsonSyntaxException e) {
			throw new RuntimeException();
		} catch (ClassCastException e) {
			throw new RuntimeException();
		}

		return new ExecutionRequest(tool, action, user, changes);

	}

	/**
	 * Trata una petición de parada de una acción concreta.
	 * 
	 * @param token
	 *            Token de uso.
	 * @param msg
	 *            Mensaje enviado por el cliente.
	 * @return Verdadero si la acción ha sido parada.
	 * @throws RuntimeException
	 *             El formato del mensaje es inválido.
	 */
	protected boolean stopRequest(String token, RLF_NetMessage msg)
			throws RuntimeException {

		String ticket;

		// 1. Autentificación del usuario.
		User user = this.activeUsers.get(token);

		ticket = msg.getAttributes().get("ticket");
		if (ticket == null)
			throw new RuntimeException();

		return this.context.getRuntimeManager().stopAction(ticket, user);

	}

	/**
	 * Ejecución del asistente. Envía los mensajes que tiene en cola, y acepta
	 * nuevas conexiones.
	 */
	@Override
	public void run() {

		ServerSocketChannel requestSocket, notificationSocket, ssc;
		SocketChannel socket;
		Selector selector;
		RLF_NetHelper net = new RLF_NetHelper();
		String attachment, token;
		LinkedList<ExecutionRequest> list = new LinkedList<ExecutionRequest>();

		try {
			requestSocket = ServerSocketChannel.open();
			requestSocket.socket().setReuseAddress(true);
			notificationSocket = ServerSocketChannel.open();
			notificationSocket.socket().setReuseAddress(true);
			selector = Selector.open();
			requestSocket.configureBlocking(false);
			notificationSocket.configureBlocking(false);
			requestSocket.socket().bind(
					new InetSocketAddress(this.context.getClientPort()));
			notificationSocket.socket().bind(
					new InetSocketAddress(this.context.getNotificationPort()));
			requestSocket.register(selector, SelectionKey.OP_ACCEPT, "REQUEST");
			notificationSocket.register(selector, SelectionKey.OP_ACCEPT,
					"NOTIFICATION");

		} catch (IOException e) {
			RLF_Log.LabLog()
					.severe("[CONNECTION] Problema de comunicación al registrar los puertos de comunicación con el cliente.");
			this.context.getKernel().signalEmergencyStop();
			return;
		}

		while (!(this.stopRequest && this.msgQueue.isEmpty())) {

			// 1. Se envían los mensajes que estan en cola.
			synchronized (this) {

				for (int i = 0; i < this.msgQueue.size(); i++) {
					User user = this.userQueue.get(i);
					RLF_NetMessage msg = this.msgQueue.get(i);
					try {

						socket = user.getNotificationSocket();
						if (socket == null)
							continue;
						else if (!socket.isConnected() || !socket.isOpen()) {
							continue;
						}

						// 1.1 Se envía el mensaje.
						net.sendMessage(msg, socket);

						// 1.2 Se cierra el socket si hay petición de stop o
						// mensaje de timeout.
						if (this.stopRequest
								|| msg.getId() == RLF_NetMessageID.TIMEOUT) {
							socket.close();
							user.setNotificationSocket(null);
						}

					} catch (IOException ex) {
					}

				}

				this.msgQueue.clear();
				this.userQueue.clear();

			}

			if (this.stopRequest)
				continue;

			// 2. Se atienden las peticiones.
			try {
				if (selector.select(DELAY) <= 0)
					continue;
			} catch (IOException e) {
				RLF_Log.LabLog()
						.warning(
								"[CONNECTION] Problema con el selector del asistente de conexión.");
				this.context.getKernel().signalEmergencyStop();
				continue;
			}

			Iterator<SelectionKey> it = (selector.selectedKeys()).iterator();

			while (it.hasNext()) {
				SelectionKey key = (SelectionKey) it.next();

				// 2.1 Aceptan nuevas peticiones.
				if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {

					ssc = (ServerSocketChannel) key.channel();
					try {

						socket = ssc.accept();
						socket.configureBlocking(false);

						socket.register(selector, SelectionKey.OP_READ,
								key.attachment());

					} catch (IOException ex) {
					}

					// 2.2 Se leen las peticiones.
				} else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {

					socket = (SocketChannel) key.channel();
					if(!key.isValid()) continue;

					if (!socket.isConnected() || !socket.isOpen()) {
						it.remove();
						continue;
					}

					attachment = (String) key.attachment();

					RLF_NetMessage out = new RLF_NetMessage();
					RLF_NetMessage msg;

					if ((msg = net.reciveMessage(socket)) == null) {
						it.remove();
						key.cancel();
						continue;
					}
					HashMap<String, String> attributes = msg.getAttributes();

					synchronized (this) {
						// 2.2.1 Autentificar un socket de peticiones.
						if (attachment.compareTo("REQUEST") == 0) {
							if (msg.getId() != RLF_NetMessageID.AUTH) {
								out.setId(RLF_NetMessageID.AUTHFAIL);
							} else if ((token = attributes.get("token")) == null) {
								out.setId(RLF_NetMessageID.FORMATERROR);
							} else if (!this.activeUsers.containsKey(token)) {
								out.setId(RLF_NetMessageID.AUTHFAIL);
							} else {

								key.attach(token);
								this.activeUsers.get(token).setRequestSocket(
										socket);
								out.setId(RLF_NetMessageID.OK);

							}
							net.sendMessage(out, socket);

							it.remove();
							continue;

							// 2.2.2 Autentificar un socket de
							// notificaciones.
						} else if (attachment.compareTo("NOTIFICATION") == 0) {

							if (msg.getId() != RLF_NetMessageID.AUTH) {
								out.setId(RLF_NetMessageID.AUTHFAIL);
							} else if ((token = attributes.get("token")) == null) {
								out.setId(RLF_NetMessageID.FORMATERROR);
							} else if (!this.activeUsers.containsKey(token)) {
								out.setId(RLF_NetMessageID.AUTHFAIL);
							} else {
								this.activeUsers.get(token)
										.setNotificationSocket(socket);
								out.setId(RLF_NetMessageID.OK);
							}
							net.sendMessage(out, socket);

							key.cancel();
							it.remove();
							continue;

						}
					}

					ExecutionRequest ex = null;

					try {
						if (msg.getId() == RLF_NetMessageID.EXEC_FINISH) {
							if (stopRequest(attachment, msg)) {
								out.setId(RLF_NetMessageID.OK);
							} else {
								out.setId(RLF_NetMessageID.NO);
							}
						} else {

							if ((ex = createRequest(attachment, msg)) != null) {
								list.add(ex);
								out.setId(RLF_NetMessageID.OK);
							} else {
								out.setId(RLF_NetMessageID.AUTHFAIL);
							}

						}
					} catch (RuntimeException e) {
						out.setId(RLF_NetMessageID.FORMATERROR);
					}

					net.sendMessage(out, socket);

				}

				it.remove();

			}

			// 3. Se envían las peticiones al asistente de ejecución.
			for (ExecutionRequest ex : list) {
				HashMap<String, String> attrs = this.context.getRuntimeManager().addRequest(ex); 
				if (attrs != null) {
					RLF_NetMessage msg = new RLF_NetMessage(RLF_NetMessageID.EXEC, attrs);
					synchronized (this) {
						this.msgQueue.add(msg);
						this.userQueue.add(ex.getUser());
					}
				}
			}
			list.clear();

		}

		try {
			requestSocket.close();
			notificationSocket.close();
			selector.close();
		} catch (IOException e) {
		}

	}

	/**
	 * Clase de la tarea específica de timeout del usuario.
	 */
	private class TimeoutTask extends TimerTask {

		// Atributos:
		/** Usuario. */
		private User user;
		/** Contexto. */
		private LabContext context;

		// Constructor:
		/**
		 * Constructor de la tarea.
		 * 
		 * @param context
		 *            Contexto del laboratorio.
		 * @param user
		 *            Usuario.
		 */
		public TimeoutTask(LabContext context, User user) {
			this.user = user;
			this.context = context;
		}

		// Métodos varios:
		/**
		 * Tarea a ejecutar.
		 */
		public void run() {

			if (this.context.getRuntimeManager() == null
					|| this.context.getConnectionManager() == null)
				return;

			// 1. Se paran los procesos en ejecución.
			this.context.getRuntimeManager().disconnectUser(this.user);

			// 2. Se informa al usuario de que su tiempo ha excedido.
			this.context.getConnectionManager().sendTimeoutMsg(this.user);

			// 3. Se avisa al proveedor.
			this.context.getKernel().userTimeout(this.user.getToken());
			user.stopTimer();

		}
	}

}
