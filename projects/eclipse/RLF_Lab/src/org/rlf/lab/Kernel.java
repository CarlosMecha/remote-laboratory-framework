/**
 * Hilo principal del Lab.
 */
package org.rlf.lab;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

import org.jdom.Document;
import org.rlf.lab.connection.ConnectionManager;
import org.rlf.lab.connection.user.ExecutionRequest;
import org.rlf.lab.data.DBHelper;
import org.rlf.lab.data.XMLHelper;
import org.rlf.lab.tool.Tool;
import org.rlf.lab.tool.ToolManager;
import org.rlf.lab.tool.runtime.RuntimeManager;
import org.rlf.log.RLF_Log;
import org.rlf.net.RLF_NetHelper;
import org.rlf.net.message.RLF_NetMessage;
import org.rlf.net.message.RLF_NetMessageID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * Hilo principal del laboratorio. Atiende las peticiones del gestor remoto y
 * del proveedor.
 * 
 * @author Carlos A. Rodriguez Mecha
 * @version 0.1
 */
public class Kernel extends Thread {

	// Constantes:
	/** (Provisional) Clave del API del LabManager. */
	private final static String LABMANAGER_KEY = "Communication";
	/** Clave para la parada de emergencia. */
	private final static String EMERGENCY_KEY = "Emergency!";
	/** Usuario para la conexión al proveedor. */
	public static String Provider_User;
	/** Contraseña para la conexión al proveedor. */
	public static String Provider_Pass;
	/** Tiempo de muestro en milisegundos. */
	public static int DELAY = 250;

	// Atributos:
	/** Contexto actual de la aplicación. */
	private LabContext context;
	/** Indica que el laboratorio debe ser parado. */
	private boolean stop;
	/** Señal de alguno de los hilos hijos de parada de emergencia. */
	private boolean emergencyStop;

	/** Instancia. */
	private static Kernel instance;

	// Constructor:
	/**
	 * Constructor del hilo.
	 * 
	 * @param context
	 *            Contexto actual.
	 */
	private Kernel(LabContext context) {
		super("Kernel");
		this.context = context;
		this.stop = false;
		this.emergencyStop = false;

	}

	// Métodos del laboratorio:
	/**
	 * Obtiene el estado de las herramientas. Es utilizado por el directorio sin
	 * identificación.
	 * 
	 * @return Lista de herramientas con su estado. El identificador 0
	 *         representa el propio laboratorio.
	 */
	protected HashMap<String, String> status() {

		ToolManager m = this.context.getToolManager();
		HashMap<String, String> status = new HashMap<String, String>();

		if (this.context.isArmed()) {
			status.put("0", "Armed");
			for (Entry<Integer, String> entry : m.status().entrySet()) {
				status.put(entry.getKey().toString(), entry.getValue());
			}
		} else {
			status.put("0", "Disarmed");
		}
		return status;

	}

	/**
	 * Arma el laboratorio. Activa las herramientas y deja a la espera a los
	 * distintos gestores. A partir de aquí, puede recibir peticiones de los
	 * clientes. Si ocurre algún error de activación, el laboratorio debería
	 * pararse por completo.
	 * 
	 * @return Verdadero si ha podido activarse. Falso si ya estaba activo o si
	 *         ha ocurrido algún error. Mirar los logs.
	 */
	protected boolean arm() {

		if (this.context.isArmed())
			return false;

		ToolManager toolM = this.context.getToolManager();

		// 1. Se obtienen los datos de las herramientas.
		toolM.reset();

		if (!toolM.readTools()) {
			return false;
		}

		// 2. Se crean los asistentes de conexión y de ejecución.
		RuntimeManager runtimeM = new RuntimeManager(this.context);
		ConnectionManager connectionM = new ConnectionManager(this.context);
		this.context.setRuntimeManager(runtimeM);
		this.context.setConnectionManager(connectionM);

		// 3. Se añaden las herramientas a los gestores.
		for (Entry<Integer, Tool> e : toolM.getTools().entrySet()) {
			runtimeM.addTool(e.getValue());
		}

		// 4. Se añaden los reseteadores.
		for (Entry<Integer, Tool> e : toolM.getTools().entrySet()) {
			runtimeM.addRequest(new ExecutionRequest(e.getKey(), Tool.RESETTER,
					null, null));
		}

		// 5. Se avisa al proveedor.
		DBHelper db = new DBHelper();
		Connection conn = db.connectToProvider(this.context.getProvider(),
				this.context.getProviderPort(), Provider_User, Provider_Pass);

		if (conn == null)
			return false;

		try {

			CallableStatement stmt = conn.prepareCall("{CALL armlab(?)}");
			stmt.setString("lab_name", this.context.getName());
			stmt.execute();
			stmt.close();
			db.close(conn);

		} catch (SQLException e) {
			RLF_Log.LabLog().severe("No se puede conectar con el proveedor.");
			db.close(conn);
			return false;
		}

		// 6. Se ejecutan los managers.
		runtimeM.start();
		connectionM.start();

		this.context.setArmed(true);
		RLF_Log.LabLog().info("Armado a las " + (new Date()).toString());

		return true;
	}

	/**
	 * Desactiva el laboratorio. Se realiza una parada completa de los
	 * asistentes de conexión y ejecución, pero no se cierra por completo el
	 * laboratorio. Es útil para insertar nuevas herramientas y hacer
	 * mantenimiento.
	 * 
	 * @return Verdadero si ha podido desactivarse.
	 */
	protected boolean disarm() {

		if (!this.context.isArmed())
			return false;

		RuntimeManager runtimeM = this.context.getRuntimeManager();
		ConnectionManager connectionM = this.context.getConnectionManager();

		// 1. Se envía una señal de parada segura.
		this.context.stop();
		runtimeM.stopRuntimes();
		connectionM.stopConnections();

		// 2. Se avisa al proveedor.
		DBHelper db = new DBHelper();
		Connection conn = db.connectToProvider(this.context.getProvider(),
				this.context.getProviderPort(), Provider_User, Provider_Pass);

		if (conn == null) {
			RLF_Log.LabLog().severe(
					"[DISARMLAB] No se puede conectar con el proveedor.");
			this.stop = true;
			return false;
		}

		try {

			CallableStatement stmt = conn.prepareCall("{CALL disarmlab(?)}");
			stmt.setString("lab_name", this.context.getName());
			stmt.execute();
			stmt.close();
			db.close(conn);

		} catch (SQLException e) {
			RLF_Log.LabLog().severe("No se puede conectar con el proveedor.");
			db.close(conn);
			this.stop = true;
			return false;
		}

		// 3. Se espera un máximo de 2 minutos para la desconexión total de los
		// hilos.
		try {
			runtimeM.join(2 * 60 * 1000);
			connectionM.join(1000);

		} catch (InterruptedException e) {
		}

		// 4. Se eliminan las instancias.
		this.context.setRuntimeManager(null);
		this.context.setConnectionManager(null);

		this.context.setArmed(false);
		RLF_Log.LabLog().info("Desarmado a las " + (new Date()).toString());
		return true;
	}

	/**
	 * Programa la parada del kernel. Esto sólo se hará si ya está desarmado el
	 * laboratorio, si no no tendrá ningún efecto. La parada es total y deberá
	 * arrancarse de nuevo el demonio.
	 * 
	 * @return Verdadero si es posible parar el laboratorio por completo.
	 */
	protected boolean stopLab() {

		// 1. Si está armado no hace nada.
		if (this.context.isArmed())
			return false;

		// 2. Avisa al proveedor.
		DBHelper db = new DBHelper();
		Connection conn = db.connectToProvider(this.context.getProvider(),
				this.context.getProviderPort(), Provider_User, Provider_Pass);

		if (conn == null) {
			RLF_Log.LabLog().severe(
					"[STOPLAB] No se puede conectar con el proveedor.");
			return false;
		}

		try {

			CallableStatement stmt = conn.prepareCall("{CALL stoplab(?)}");
			stmt.setString("lab_name", this.context.getName());
			stmt.execute();
			stmt.close();
			db.close(conn);

		} catch (SQLException e) {
			RLF_Log.LabLog().severe(
					"[STOPLAB] No se puede conectar con el proveedor.");
			db.close(conn);
			return false;
		}

		this.stop = true;
		return true;

	}

	/**
	 * Registra una herramienta para poder utilizarla cuando se active el
	 * laboratorio. Utiliza el fichero XML proporcionado por el administrador.
	 * 
	 * @param xml
	 *            Nombre con la ruta del fichero xml donde se describe la
	 *            herramienta.
	 * @return Devuelve el identificador del servicio así como su clave de forma
	 *         <id, key>. Null si ha ocurrido algún error.
	 */
	protected HashMap<String, String> registryTool(String xml) {

		if (this.context.isArmed())
			return null;

		int id = 0;
		String key = new String();
		File docXML = new File(xml);
		if (!docXML.exists() || !docXML.isFile())
			return null;

		// 1. Se pide la clave y el identificador al proveedor.
		DBHelper db = new DBHelper();
		Connection conn = db.connectToProvider(this.context.getProvider(),
				this.context.getProviderPort(), Provider_User, Provider_Pass);

		if (conn == null)
			return null;

		try {

			CallableStatement stmt = conn.prepareCall("{CALL toolkey()}");
			ResultSet rs = stmt.executeQuery();
			if (!rs.next()) {
				rs.close();
				stmt.close();
				db.close(conn);
				return null;
			}

			id = rs.getInt("id");
			key = rs.getString("toolkey");

			rs.close();
			stmt.close();

		} catch (SQLException e) {
			RLF_Log.LabLog().severe("No se puede conectar con el proveedor.");
			db.close(conn);
			return null;
		}

		// 2. Se valida el fichero.
		XMLHelper h = new XMLHelper();
		Document doc = h.read(docXML);
		if (doc == null) {
			db.close(conn);
			return null;
		}
		// 3. Se lee.
		try {
			if (!this.context.getToolManager().registry(id, key, doc, conn)) {
				return null;
			}
			db.close(conn);
		} catch (SQLException e) {
			RLF_Log.LabLog().severe(
					"No se ha podido realizar la recuperación de datos.");
			db.close(conn);
			this.stop = true;
			return null;
		}

		HashMap<String, String> attr = new HashMap<String, String>();
		attr.put(new Integer(id).toString(), key);
		return attr;

	}

	/**
	 * Elimina una herramienta del laboratorio. Para ello es necesario su
	 * identificador y la clave que se le proporcionó. Esta operación sólo puede
	 * ser realizada si el laboratorio está desarmado.
	 * 
	 * @param id
	 *            Identificador de la herramienta.
	 * @param key
	 *            Clave del servicio.
	 * @return Verdadero si ha podido eliminar la herramienta.
	 */
	protected boolean dropTool(int id, String key) {

		if (this.context.isArmed())
			return false;

		// 1. Se elimina la herramienta.
		if (!this.context.getToolManager().drop(id, key)) {
			return false;
		}

		// 2. Se elimina del proveedor.
		DBHelper db = new DBHelper();
		Connection conn = db.connectToProvider(this.context.getProvider(),
				this.context.getProviderPort(), Provider_User, Provider_Pass);

		if (conn == null)
			return false;

		try {

			CallableStatement stmt = conn.prepareCall("{CALL droptool (?)}");
			stmt.setInt("tid", id);
			stmt.execute();
			stmt.close();
			db.close(conn);
			return true;

		} catch (SQLException e) {
			RLF_Log.LabLog().severe(
					"[DROPTOOL] No se puede conectar con el proveedor.");
			db.close(conn);
			return false;
		}

	}

	/**
	 * Trata la operación introducida enviada por un gestor remoto. Si la
	 * operación no es válida, se le devuelve una respuesta NULL.
	 * 
	 * @param msg
	 *            Mensaje recibido.
	 * @return Mensaje a enviar al LabManager.
	 */
	protected RLF_NetMessage processLabManagerRequest(RLF_NetMessage msg) {

		RLF_NetMessage out = new RLF_NetMessage();
		RLF_Log.LabLog().info(
				"Operación recibida del LabManager: " + msg.getId().toString());
		HashMap<String, String> attributes = msg.getAttributes();

		if (attributes.get("key") == null || attributes.get("hash") == null) {
			out.setId(RLF_NetMessageID.NO);
			return out;
		}

		// 1. Se obtiene la clave.
		if (!authManager(attributes.get("key"))) {
			out.setId(RLF_NetMessageID.AUTHFAIL);
			RLF_Log.LabLog().info(
					"Fallo de autentificación del gestor: "
							+ attributes.get("key"));
			return out;
		}

		if (!authAdmin(attributes.get("hash"))) {
			out.setId(RLF_NetMessageID.AUTHFAIL);
			RLF_Log.LabLog().info(
					"Fallo de autentificación de administrador: "
							+ attributes.get("hash"));
			return out;
		}

		switch (msg.getId()) {

		// Estado del Lab:
		case LABSTATUS:
			out.setId(RLF_NetMessageID.LABSTATUS);
			out.setAttributes(status());
			break;

		// Activación:
		case ARMLAB:

			if (this.context.isArmed()) {
				out.setId(RLF_NetMessageID.NO);
				break;
			}

			if (!arm()) {
				out.setId(RLF_NetMessageID.NO);
				emergencyStop();
				break;
			}

			out.setId(RLF_NetMessageID.OK);

			break;

		// Desarme:
		case DISARMLAB:

			if (!disarm()) {
				out.setId(RLF_NetMessageID.NO);
				break;
			}

			out.setId(RLF_NetMessageID.OK);

			break;

		// Registro de una herramienta:
		case REGISTRYTOOL:

			if (this.context.isArmed()) {
				out.setId(RLF_NetMessageID.NO);
				break;
			}

			if (attributes.get("path") == null) {
				out.setId(RLF_NetMessageID.NO);
				break;
			}

			attributes = registryTool(attributes.get("path"));
			if (attributes == null) {
				out.setId(RLF_NetMessageID.NO);
				break;
			}

			out.setId(RLF_NetMessageID.TOOLKEY);
			out.setAttributes(attributes);

			break;

		// Eliminación de una herramienta:
		case DROPTOOL:

			if (this.context.isArmed()) {
				out.setId(RLF_NetMessageID.NO);
				break;
			}

			if (attributes.get("id") == null
					|| attributes.get("toolkey") == null) {
				out.setId(RLF_NetMessageID.NO);
				break;
			}
			if (!dropTool(Integer.parseInt(attributes.get("id")),
					attributes.get("toolkey"))) {
				out.setId(RLF_NetMessageID.NO);
				break;
			}

			out.setId(RLF_NetMessageID.OK);

			break;

		// Parada de emergencia:
		case EMERGENCYSTOP:

			if (attributes.get("!") == null) {
				out.setId(RLF_NetMessageID.NO);
				break;
			}

			// 1. Se comprueba la clave de emergencia.
			if (attributes.get("!").compareTo(EMERGENCY_KEY) != 0) {
				out.setId(RLF_NetMessageID.NO);
				break;
			}

			// 2. Se inicia la rutina.
			emergencyStop();
			out.setId(RLF_NetMessageID.OK);

			break;

		// Parada total del Lab:
		case STOPLAB:

			if (!stopLab()) {
				out.setId(RLF_NetMessageID.NO);
				break;
			}

			out.setId(RLF_NetMessageID.OK);

			break;

		default:
			out.setId(RLF_NetMessageID.NULL);
		}

		RLF_Log.LabLog().info(
				"Operación tratada del LabManager: " + out.getId().toString());
		return out;

	}

	/**
	 * Trata la operación introducida enviada por el proveedor. Si la operación
	 * no es válida, se le devuelve una respuesta NULL. Los mensajes no
	 * necesitan autentificación.
	 * 
	 * @param msg
	 *            Mensaje recibido.
	 * @return Mensaje a enviar al proveedor.
	 */
	protected RLF_NetMessage processProviderRequest(RLF_NetMessage msg) {

		RLF_NetMessage out = new RLF_NetMessage();
		RLF_Log.LabLog().info(
				"Operación recibida del proveedor: " + msg.getId().toString());

		switch (msg.getId()) {

		// Estado del Lab:
		case LABSTATUS:
			out.setId(RLF_NetMessageID.LABSTATUS);
			out.setAttributes(status());
			break;

		// Token de usuario:
		case TOKEN:

			JsonParser parser = new JsonParser();

			if (!this.context.isArmed()) {
				out.setId(RLF_NetMessageID.NO);
				break;
			}

			LinkedList<Integer> tools = new LinkedList<Integer>();
			String token = msg.getAttributes().get("token");
			int timeout = Integer.parseInt(msg.getAttributes().get("timeout"));
			JsonArray array = parser.parse(msg.getAttributes().get("tools"))
					.getAsJsonArray();
			for (JsonElement e : array)
				tools.add(e.getAsInt());

			this.context.getConnectionManager().addUser(token, timeout, tools);
			
			out.setId(RLF_NetMessageID.OK);

			break;

		// Desconexión voluntaria del usuario:
		case LOGOUT:

			if (!this.context.isArmed()) {
				out.setId(RLF_NetMessageID.NO);
				break;
			}

			String userToken = msg.getAttributes().get("token");
			this.context.getConnectionManager().removeUser(userToken);

			out.setId(RLF_NetMessageID.OK);
			break;

		default:
			out.setId(RLF_NetMessageID.NULL);
		}

		RLF_Log.LabLog().info(
				"Operación tratada del LabManager: " + out.getId().toString());
		return out;

	}

	/**
	 * Obtiene la instancia del hilo de control del laboratorio.
	 * 
	 * @param context
	 *            Contexto actual. Sólo se utilizará la primera vez que se llame
	 *            al método.
	 */
	public static Kernel Instance(LabContext context) {
		if (instance == null) {
			instance = new Kernel(context);
			instance.context.setKernel(instance);
		}

		return instance;
	}

	// Métodos varios:
	/**
	 * Avisa al proveedor que un usuario con un token concreto ha superado el
	 * tiempo máximo de conexión con el laboratorio.
	 * 
	 * @param token
	 *            Token de uso del usuario.
	 */
	public void userTimeout(String token) {

		// 1. Se avisa al proveedor.
		DBHelper db = new DBHelper();
		Connection conn = db.connectToProvider(this.context.getProvider(),
				this.context.getProviderPort(), Provider_User, Provider_Pass);

		if (conn == null) {
			RLF_Log.LabLog().severe(
					"[USERTIMEOUT] No se puede conectar con el proveedor.");
			return;
		}

		try {

			CallableStatement stmt = conn.prepareCall("{CALL timeout(?)}");
			stmt.setString("client_token", token);
			stmt.execute();
			stmt.close();

		} catch (SQLException e) {
			RLF_Log.LabLog().severe(
					"[USERTIMEOUT] No se puede conectar con el proveedor.");
		} finally {
			db.close(conn);
		}

	}

	/**
	 * Envía una señal al kernel para realizar una parada de emergencia.
	 */
	public synchronized void signalEmergencyStop() {
		this.emergencyStop = true;
	}

	/**
	 * Notifica una parada de emergencia a todos los hilos del laboratorio.
	 * Implica que cierra todos los procesos abiertos y que no aceptará más
	 * peticiones. Además, a los usuarios se les informa de esta parada pero no
	 * de los resultados de las ejecuciones. Una vez hecho esto, se cierra la
	 * ejecución por completo del laboratorio. No hay retorno una vez invocado
	 * este método.
	 */
	protected void emergencyStop() {

		RLF_Log.LabLog().info("[EMERGENCY] Parada de emergencia.");

		if (!this.context.isArmed()) {
			this.stop = true;
			return;
		}

		RuntimeManager runtimeM = this.context.getRuntimeManager();
		ConnectionManager connectionM = this.context.getConnectionManager();

		// 1. Se envía una señal.
		this.context.stop();
		runtimeM.stopRuntimes();
		connectionM.stopConnections();

		// 2. Se avisa al proveedor.
		DBHelper db = new DBHelper();
		Connection conn = db.connectToProvider(this.context.getProvider(),
				this.context.getProviderPort(), Provider_User, Provider_Pass);

		if (conn != null) {

			try {

				CallableStatement stmt = conn
						.prepareCall("{CALL emergency (?)}");
				stmt.setString("lab_name", this.context.getName());
				stmt.execute();
				stmt.close();

			} catch (SQLException e) {
				RLF_Log.LabLog().severe(
						"[EMERGENCY] No se puede conectar con el proveedor.");

			} finally {
				db.close(conn);
			}

		} else {
			RLF_Log.LabLog().severe(
					"[EMERGENCY] No se puede conectar con el proveedor.");
		}

		// 3. Se espera un máximo de 1 minuto para la desconexión total de los
		// hilos.
		try {
			runtimeM.join(1 * 60 * 1000);
			connectionM.join(1000);

			// 4. Si aún están activos se interrumpen.
			if (runtimeM.isAlive() || connectionM.isAlive()) {
				runtimeM.interrupt();
				connectionM.interrupt();
			}

		} catch (InterruptedException e) {
		}

		// 4. Se eliminan las instancias.
		this.context.setRuntimeManager(null);
		this.context.setConnectionManager(null);
		RLF_Log.LabLog()
				.info("PARADA DE EMERGENCIA " + (new Date()).toString());
		this.stop = true;

	}

	/**
	 * Método de ejecución del hilo.
	 */
	@Override
	public void run() {

		ServerSocketChannel labManagerS = null, providerS = null, ssc;
		SocketChannel socket;
		RLF_NetMessage msg, reply;
		Selector selector;
		Iterator<SelectionKey> it;
		RLF_NetHelper net = new RLF_NetHelper();

		try {

			selector = Selector.open();

			// 1. Se activa el puerto de escucha del LabManager.
			labManagerS = ServerSocketChannel.open();
			labManagerS.socket().setReuseAddress(true);
			labManagerS.configureBlocking(false);
			labManagerS.socket()
					.bind(new InetSocketAddress(
							this.context.getLabManagerPort()), 10);
			labManagerS.register(selector, SelectionKey.OP_ACCEPT,
					new Character('m'));

			// 2. Se activa el puerto de escucha del proveedor.
			providerS = ServerSocketChannel.open();
			providerS.socket().setReuseAddress(true);
			providerS.configureBlocking(false);
			providerS.socket()
					.bind(new InetSocketAddress(
							this.context.getProviderRequestPort()), 10);
			providerS.register(selector, SelectionKey.OP_ACCEPT, new Character(
					'p'));

		} catch (IOException e) {
			RLF_Log.LabLog().severe(
					"[STARTLAB] Problema al crear los sockets de escucha.");
			try {
				labManagerS.close();
				providerS.close();
			} catch (Exception ex) {

			}
			return;
		}

		// 3. Notificación al proveedor.
		if (!startLab())
			return;

		while (true) {

			// 4. Parada total.
			synchronized (this) {

				if (this.stop) {
					try {
						labManagerS.close();
						providerS.close();
					} catch (Exception e) {
					}

					break;
				}

				if (this.emergencyStop) {
					emergencyStop();
					continue;
				}

			}

			// 3. Se espera a la recepción de mensajes.
			try {
				if (selector.select(DELAY) <= 0)
					continue;
			} catch (IOException e) {
				RLF_Log.LabLog().severe("[KERNEL] Problema con el selector.");
				emergencyStop();
				continue;
			}

			it = (selector.selectedKeys()).iterator();
			while (it.hasNext()) {
				SelectionKey key = (SelectionKey) it.next();

				// 3.1 Se acepta una petición entrante por tipo.
				if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
					ssc = (ServerSocketChannel) key.channel();

					try {
						socket = ssc.accept();
						socket.configureBlocking(false);
						socket.register(selector, SelectionKey.OP_READ,
								key.attachment());

					} catch (IOException e) {
					}
					// 3.2 Obtiene los mensajes enviados.
				} else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
					socket = (SocketChannel) key.channel();
					if ((msg = net.reciveMessage(socket)) == null) {
						try {
							socket.close();
						} catch (IOException e) {
						}
						it.remove();
						continue;
					}

					// 3.2.1 Tratamiento del mensaje.
					synchronized (this) {
						if (((Character) key.attachment()) == 'p') {
							reply = processProviderRequest(msg);
						} else
							reply = processLabManagerRequest(msg);
					}

					// 3.2.2 Envío de la respuesta.
					if (!net.sendMessage(reply, socket)) {
						RLF_Log.LabLog()
								.warning(
										"[EXCEPTION] No se ha podido enviar el mensaje al receptor.");
					}

					try {
						socket.close();
					} catch (IOException e) {
					}

				}

				it.remove();

			}

		}

	}

	// Métodos varios:
	/**
	 * Notifica al proveedor que el laboratorio está conectado. Si esta
	 * operación falla, el laboratorio se desconecta.
	 * 
	 * @return Verdadero si ha podido notificar.
	 */
	private boolean startLab() {

		DBHelper db = new DBHelper();
		Connection conn = db.connectToProvider(this.context.getProvider(),
				this.context.getProviderPort(), Provider_User, Provider_Pass);

		if (conn == null)
			return false;

		try {

			CallableStatement stmt = conn
					.prepareCall("{CALL startlab (?, ?, ?, ?)}");
			stmt.setString("lab_name", this.context.getName());
			stmt.setInt("p_request", this.context.getProviderRequestPort());
			stmt.setInt("p_client", this.context.getClientPort());
			stmt.setInt("p_notification", this.context.getNotificationPort());
			stmt.execute();
			stmt.close();
			db.close(conn);
			return true;

		} catch (SQLException e) {
			RLF_Log.LabLog().severe(
					"[STARTLAB] No se puede conectar con el proveedor.");
			db.close(conn);
			return false;
		}

	}

	/**
	 * Autentificación de un gestor remoto.
	 * 
	 * @param key
	 *            Clave del LabManager.
	 * @return Verdadero si la autentificación ha sido correcta.
	 */
	private boolean authManager(String key) {

		if (key.compareTo(LABMANAGER_KEY) == 0)
			return true;
		else
			return false;

	}

	/**
	 * Autentificación de un administrador. Realiza la petición al proveedor
	 * para saber si el administrador está registrado como tal. Se usa para
	 * acciones llevadas a cabo por un gestor remoto.
	 * 
	 * @param hash
	 *            Hash del nombre y contraseña del administrador.
	 * @return Verdadero si la autentificación ha sido correcta.
	 */
	private boolean authAdmin(String hash) {

		DBHelper db = new DBHelper();
		Connection conn = db.connectToProvider(this.context.getProvider(),
				this.context.getProviderPort(), Provider_User, Provider_Pass);

		if (conn == null) {
			RLF_Log.LabLog().severe(
					"[AUTHADMIN] No se puede conectar con el proveedor.");
			return false;
		}

		try {

			CallableStatement stmt = conn.prepareCall("{CALL authadmin (?)}");
			stmt.setString("admin_hash", hash);
			ResultSet rs = stmt.executeQuery();
			if (!rs.next() || rs.getInt(1) == 0) {
				rs.close();
				stmt.close();
				db.close(conn);
				return false;
			}

			rs.close();
			stmt.close();
			db.close(conn);
			return true;

		} catch (SQLException e) {
			RLF_Log.LabLog().severe(
					"[AUTHADMIN] No se puede conectar con el proveedor.");
			db.close(conn);
			return false;
		}

	}

}
