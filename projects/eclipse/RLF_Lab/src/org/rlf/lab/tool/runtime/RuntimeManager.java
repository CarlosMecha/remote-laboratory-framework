/**
 * Hilo RuntimeManager.
 */
package org.rlf.lab.tool.runtime;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.rlf.lab.LabContext;
import org.rlf.lab.connection.user.ExecutionRequest;
import org.rlf.lab.connection.user.User;
import org.rlf.lab.exception.DBException;
import org.rlf.lab.tool.Tool;
import org.rlf.log.RLF_Log;

/**
 * Asistente que se encarga de almacenar y realizar por orden las peticiones de
 * ejecución de las acciones. Contiene un número máximo de acciones a ejecutar a
 * la vez.
 * 
 * @author Carlos A. Rodriguez Mecha
 * @version 0.1
 */
public class RuntimeManager extends Thread {

	// Constantes:
	/** Tiempo de muestreo en milisegundos. */
	public int DELAY = 500;
	/** Puerto del cual se pueden establecer los otros puertos. */
	public int PORT_BASE = 64000;

	// Atributos:
	/** Comandos en ejecución. Cada posición corresponde con un "procesador". */
	private Action[] running;
	/** Gestores de los diferentes servicios. */
	private HashMap<Integer, ToolRuntime> tools;
	/** Contexto de la aplicación. */
	private LabContext context;
	/** Petición de parada total. */
	private boolean stopRequest;
	/**
	 * Puertos de las acciones de herramientas de datos en ejecución para enviar
	 * a los nuevos usuarios que se conecten a ellas.
	 */
	private HashMap<String, Integer> runningDataToolsPorts;
	/**
	 * Lista con el orden de ejecución de las herramientas. Contiene los
	 * identificadores de las herramientas que van obteniendo peticiones por
	 * orden, para que sean ejecutadas de la misma forma.
	 */
	private LinkedList<Integer> queue;

	// Constructor:
	/**
	 * Constructor del asistente.
	 * 
	 * @param context
	 *            Contexto del laboratorio.
	 */
	public RuntimeManager(LabContext context) {
		super("RuntimeManager");
		this.running = new Action[context.getProcessors()];
		this.stopRequest = false;
		this.tools = new HashMap<Integer, ToolRuntime>();
		this.runningDataToolsPorts = new HashMap<String, Integer>();
		this.context = context;
		this.queue = new LinkedList<Integer>();

	}

	// Métodos varios:
	/**
	 * Añade una nueva herramienta para ser ejecutada. Sólo será efectivo si el
	 * hilo NO está en ejecución y no hay una petición de parada.
	 * 
	 * @param tool
	 *            Herramienta.
	 */
	public synchronized void addTool(Tool tool) {
		if (this.isAlive() || this.stopRequest)
			return;

		tools.put(tool.getId(), new ToolRuntime(tool));
	}

	/**
	 * Elimina una herramienta para que no vuelva a ser ejecutada. Sólo será
	 * efectivo si el hilo NO está en ejecución y no hay una petición de parada.
	 * 
	 * @param id
	 *            Identificador de la herramienta.
	 */
	public synchronized void removeTool(int id) {
		if (this.isAlive() || this.stopRequest)
			return;
		this.tools.remove(id);
	}

	/**
	 * Añade una petición para poder ser ejecutada. Si hay una petición de
	 * parada no tiene efecto.
	 * 
	 * @return Atributos del mensaje que hay que enviar al usuario si la acción
	 *         ya está siendo realizada por otro usuario (en el caso de las
	 *         herramientas de datos)para que se conecte a esa herramienta. Null
	 *         en caso contrario.
	 */
	public synchronized HashMap<String, String> addRequest(ExecutionRequest ex) {

		if (this.stopRequest)
			return null;

		try {
			if (tools.get(ex.getIdTool()).add(ex)) {
				queue.add(ex.getIdTool());
			}
			return null;
		} catch (RuntimeException e) {
			for (int i = 0; i < running.length; i++) {
				Action a = running[i];
				if (a == null)
					continue;
				if (a.getToolId() == ex.getIdTool()) {
					HashMap<String, String> attrs = new HashMap<String, String>();
					attrs.put("tool", new Integer(ex.getIdTool()).toString());
					attrs.put("action", ex.getActionName());
					attrs.put("ticket", a.getTicket());
					attrs.put("portOut",
							runningDataToolsPorts.get(a.getTicket()).toString());
					return attrs;
				}
			}
			return null;
		}

	}

	/**
	 * Se utiliza cuando al usuario introducido se le ha acabado el tiempo o se
	 * ha desconectado. Cerrará las ejecuciones iniciadas por él y le eliminará
	 * de las herramientas de datos. Si esas herramientas ya no tienen más
	 * usuarios que las usen se cierran. Por último se introducen los
	 * reseteadores para las herramientas modificadas.
	 * 
	 * @param user
	 *            Usuario con timeout.
	 */
	public synchronized void disconnectUser(User user) {

		// 1. Se comprueba si hay algún comando ejecutado por el usuario.
		for (int i = 0; i < running.length; i++) {
			if (running[i] == null)
				continue;
			ToolRuntime s = tools.get(running[i].getToolId());
			ExecutionRequest ex = s.getRunning();
			if (!ex.containsUser(user))
				continue;

			// 1.1 Si es herramienta de datos.
			if (s.getTool().isDataTool()) {
				if (ex.removeUser(user))
					continue;
				runningDataToolsPorts.remove(running[i].getTicket());
			}

			// 2. Se para la acción.
			s.finish();
			running[i].stopAction();
			s.add(new ExecutionRequest(s.getId(), Tool.RESETTER, null, null));
			queue.add(s.getId());

			try {
				running[i].join(DELAY * 2);
			} catch (InterruptedException e) {
			}
			running[i] = null;

		}

		// 3. Se comprueban las peticiones en los servicios.
		for (ToolRuntime s : tools.values()) {

			if (!user.canUse(s.getId()))
				continue;

			if (!s.getTool().isDataTool())
				s.clear();
			else
				s.clearDataTool(user);

		}

	}

	/**
	 * Para una de las ejecuciones actuales. Una vez parada, se añade el
	 * resetter correspondiente y no se avisa al usuario del final de la
	 * ejecución.
	 * 
	 * @param ticket
	 *            Ticket de la ejecución.
	 * @param user
	 *            Usuario que ha invocado la parada.
	 * @return Verdadero si ha parado la ejecución. Falso en caso de que ya haya
	 *         acabado.
	 */
	public synchronized boolean stopAction(String ticket, User user) {

		// 1. Se comprueba si hay algún comando ejecutado por el usuario.
		for (int i = 0; i < running.length; i++) {
			if (running[i] == null)
				continue;
			if (running[i].getTicket().compareTo(ticket) != 0)
				continue;

			ToolRuntime s = tools.get(running[i].getToolId());
			ExecutionRequest ex = s.getRunning();
			if (!ex.containsUser(user))
				return false;

			// 1.1 Si es herramienta de datos.
			if (s.getTool().isDataTool()) {
				if (ex.removeUser(user))
					return true;
				runningDataToolsPorts.remove(ticket);
			}

			// 2. Se para la acción.
			s.finish();
			running[i].stopAction();
			s.add(new ExecutionRequest(s.getId(), Tool.RESETTER, null, null));
			queue.add(s.getId());

			try {
				running[i].join(DELAY * 2);
			} catch (InterruptedException e) {
			}
			running[i] = null;

			return true;

		}

		return false;

	}

	/**
	 * Petición de parada. Se interrumpen todas las acciones en ejecución y se
	 * borran las peticiones de los clientes en espera. No tiene efecto si ya se
	 * ha parado.
	 */
	public synchronized void stopRuntimes() {

		if (!this.isAlive() || this.stopRequest)
			return;

		// 1. Para las acciones en ejecución actual.
		for (int i = 0; i < running.length; i++) {
			if (running[i] == null)
				continue;
			running[i].stopAction();

			try {
				running[i].join(DELAY * 2);
			} catch (InterruptedException e) {
			}
			running[i] = null;

		}

		// 2. Se limpian las peticiones en espera.
		for (ToolRuntime tool : tools.values()) {
			tool.clear();
			tool.finish();
		}

		this.queue.clear();
		this.stopRequest = true;
	}

	/**
	 * Ejecuta una acción. Realiza los cambios en la base de datos antes de
	 * ejecutarlo.
	 * 
	 * @param processor
	 *            Posición en el procesador.
	 * @param tool
	 *            Herramienta a ejecutar.
	 * @param ex
	 *            Petición.
	 */
	protected void runCommand(int processor, Tool tool, ExecutionRequest ex) {

		// 1. Si es una acción normal.
		if (ex.getActionName().compareTo(Tool.RESETTER) != 0) {

			// 1.1 Asignación de puertos.
			int portIn = -1, portOut = -1;
			ServerSocketChannel in = null, out = null;
			try {
				if (tool.hasInStream()) {
					in = ServerSocketChannel.open();
					in.configureBlocking(false);
					in.socket().bind(null);
					portIn = in.socket().getLocalPort();
				}
			} catch (IOException e) {
				if (in != null) {
					try {
						in.close();
					} catch (IOException ioe) {
					}
				}
				in = null;
				portIn = -1;
			}
			try {
				if (tool.hasOutStream()) {
					out = ServerSocketChannel.open();
					out.configureBlocking(false);
					out.socket().bind(null);
					portOut = out.socket().getLocalPort();

				}
			} catch (IOException e) {
				if (out != null) {
					try {
						out.close();
					} catch (IOException ioe) {
					}
				}
				out = null;
				portOut = -1;

			}

			// 1.2 Ejecución.
			try {

				running[processor] = tool.execute(ex.getActionName(), in, out,
						ex.getChanges());
			} catch (Exception e) {
				// No existe la acción requerida. Se ignora.
				ex.error();
				running[processor] = null;

				// 1.4 Envío de la notificación.
				this.context.getConnectionManager()
						.sendTicket(ex, null, -1, -1);

				return;

			}

			String ticket = running[processor].getTicket();

			// 1.4 Envío de la notificación.
			this.context.getConnectionManager().sendTicket(ex, ticket, portIn,
					portOut);

			// 1.5 Se almacena el puerto si es una herramienta de datos.
			if (tool.isDataTool()) {

				runningDataToolsPorts.put(ticket, portOut);
			}

			// 2. Reseteador. No se envía notificación.
		} else {

			try {
				running[processor] = tool.clean();
			} catch (DBException e) {
				running[processor] = null;
			}

		}

	}

	/**
	 * Obtiene los cambios realizados en la base de datos por la ejecución de la
	 * acción. A continuación se lo envía al asistente de conexión para que
	 * avise al usuario.
	 * 
	 * @param ticket
	 *            Ticket de la acción.
	 * @param ex
	 *            Petición de ejecución.
	 */
	protected void finishCommand(String ticket, ExecutionRequest ex) {

		HashMap<String, String> changes = null;

		// 1. Se obtienen los cambios.
		try {
			changes = this.tools.get(ex.getIdTool()).getTool()
					.showChanges(ex.getActionName());
		} catch (DBException e) {
			RLF_Log.LabLog().warning("Revisar herramienta " + ex.getIdTool());
		}

		ex.setChanges(changes);
		ex.finish();

	}

	/**
	 * Obtiene el estado de la acción que se ha bloqueada, así como las
	 * excepciones.
	 * 
	 * @param ticket
	 *            Ticket de la acción.
	 * @param ex
	 *            Petición de ejecución.
	 */
	protected void freezeCommand(String ticket, ExecutionRequest ex) {

		HashMap<String, String> changes = null;

		// 1. Se obtienen los cambios.
		try {
			changes = this.tools.get(ex.getIdTool()).getTool()
					.showChanges(ex.getActionName());
		} catch (DBException e) {
			RLF_Log.LabLog().warning("Revisar herramienta " + ex.getIdTool());
		}

		ex.setChanges(changes);
		ex.error();

	}

	/**
	 * El asistente va resolviendo las peticiones en orden e indica al asistente
	 * de conexión los mensajes que tiene que enviar a los clientes.
	 */
	@Override
	public void run() {

		while (!this.stopRequest) {

			HashMap<String, ExecutionRequest> list = new HashMap<String, ExecutionRequest>();

			// 1. Se comprueban los procesos en ejecución.
			synchronized (this) {

				for (int i = 0; i < running.length; i++) {
					Action c = running[i];
					if (c == null)
						continue;

					// 1.1 Si ya ha terminado.
					if (!c.isAlive()) {

						try {
							running[i].join();
						} catch (InterruptedException e) {
						}
						ToolRuntime s = tools.get(c.getToolId());
						ExecutionRequest ex = s.finish();

						// 1.2 Si es un resetter no hay notificación ni uso
						// de puertos.
						if (ex.getActionName().compareTo(Tool.RESETTER) != 0) {

							// 1.3 Si es una herramienta de datos no hay
							// notificación.
							if (!s.getTool().isDataTool()) {

								if (s.getTool().getStatus() == Tool.ToolStatus.OFF) {
									freezeCommand(c.getTicket(), ex);
								} else
									finishCommand(c.getTicket(), ex);

								list.put(c.getTicket(), ex);
							} else {
								runningDataToolsPorts.remove(c.getTicket());

							}

							if (s.getTool().getStatus() == Tool.ToolStatus.OFF) {
								s.add(new ExecutionRequest(s.getId(),
										Tool.RESETTER, null, null));
								queue.add(s.getId());
							}

						}

						running[i] = null;
					}
				}
			}

			// 2. Se envían a los usuarios los resultados.
			for (Entry<String, ExecutionRequest> e : list.entrySet()) {
				this.context.getConnectionManager().sendResultMsg(e.getKey(),
						e.getValue());
			}

			// 3. Se añaden nuevas ejecuciones si se puede.
			synchronized (this) {
				if (!queue.isEmpty()) {
					LinkedList<Integer> executionTools = new LinkedList<Integer>();
					for (Action a : running){
						if (a == null) continue;
						executionTools.add(a.getToolId());
					}
					
					for (int i = 0; i < running.length; i++) {
						if (queue.isEmpty())
							break;
						if (running[i] != null) {
							continue;
						}
						ToolRuntime s = tools.get(queue.getFirst());
						if (executionTools.contains(s.getId())) {
							continue;
						}
						
						queue.remove();

						ExecutionRequest ex = s.next();
						if (ex == null)
							continue;
						runCommand(i, s.getTool(), ex);

					}
				}
			}

			try {
				Thread.sleep(DELAY);

			} catch (InterruptedException e) {
				RLF_Log.LabLog().severe(
						"[EXCEPTION] Asistente de ejecución interrumpido.");
				stopRuntimes();
			}
		}

	}

}
