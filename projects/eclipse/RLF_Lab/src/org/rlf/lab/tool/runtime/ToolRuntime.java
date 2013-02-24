/**
 * Ejecución de una herramienta.
 */
package org.rlf.lab.tool.runtime;

import java.util.LinkedList;

import org.rlf.lab.connection.user.ExecutionRequest;
import org.rlf.lab.connection.user.User;
import org.rlf.lab.tool.Tool;

/**
 * Asistente de ejecución de cada herramienta. Contiene la lista de las acciones
 * que van a ser ejecutadas en orden y con los usuarios que realizarn esas
 * peticiones.
 * 
 * @author Carlos A. Rodriguez Mecha
 * @version 0.1
 */
public class ToolRuntime {

	// Atributos:
	/** Herramienta asociada. */
	private Tool tool;
	/** Petición en ejecución. Puede ser null. */
	private ExecutionRequest running;
	/** Lista de peticiones de esta herramienta. */
	private LinkedList<ExecutionRequest> queue;

	// Constructor:
	/**
	 * Constructor del asistente.
	 * 
	 * @param service
	 *            Servicio asociado.
	 */
	public ToolRuntime(Tool service) {
		this.tool = service;
		this.running = null;
		this.queue = new LinkedList<ExecutionRequest>();
	}

	// Métodos getters:
	/**
	 * Obtiene el identificador de la herramienta asociada. Es lo mismo que
	 * invocar getTool().getId().
	 * 
	 * @return Identificador de la herramienta.
	 */
	public int getId() {
		return this.tool.getId();
	}

	/**
	 * Obtiene la herramienta asociada.
	 * 
	 * @return Herramienta.
	 */
	public Tool getTool() {
		return tool;
	}

	/**
	 * Obtiene la petición en ejecución.
	 * 
	 * @return Petición. Puede ser null si no hay ninguna.
	 */
	public ExecutionRequest getRunning() {
		return running;
	}

	/**
	 * Obtiene la primera petición a la cola (no en ejecución).
	 * 
	 * @return Petición. Null si no hay peticiones.
	 */
	public ExecutionRequest getQueueHead() {
		if (queue.size() < 1)
			return null;
		return queue.getFirst();
	}

	// Métodos varios:
	/**
	 * Añade una petición a la cola. Si es una herramienta de datos y la
	 * petición ya está en cola o ejecutando se añade el usuario. Si es un
	 * reseteador se añade directamente a la cola de peticiones.
	 * 
	 * @param request
	 *            Petición de ejecución.
	 * @return Indica si se ha añadido como una petición nueva. Si es una
	 *         herramienta de datos y ya se estaba ejecutando la acción se
	 *         devuelve falso.
	 * @throws RuntimeException
	 *             "Excepción" que indica que la petición está actualmente en
	 *             ejecución por ser una herramienta de datos. Esto sirve para
	 *             avisar al usuario inmediatamente de que su petición está
	 *             siendo ya realizada y que puede usar la herramienta.
	 */
	public boolean add(ExecutionRequest request) throws RuntimeException {

		// 1. Si es un reseteador.
		if (request.getActionName() == Tool.RESETTER) {
			if (queue.size() > 0
					&& queue.element().getActionName() == Tool.RESETTER) {
				return true;
			} else {
				queue.push(request);
				return true;
			}
		}

		// 1. Si es una herramienta de datos.
		if (tool.isDataTool()) {

			// 1.1 Si se está ejecutando el comando.
			if ((running != null)
					&& (request.getActionName().compareTo(
							running.getActionName()) == 0)) {
				running.addUser(request.getUser());
				throw new RuntimeException();
			} else {
				for (ExecutionRequest ex : queue) {
					if (ex.getActionName() == Tool.RESETTER)
						continue;
					if (!ex.getUser().equals(request.getUser())) {
						ex.addUser(request.getUser());
					}
					return false;

				}
			}
		}

		queue.add(request);
		return true;

	}

	/**
	 * Pasa una petición a la ejecución. No realiza la operación, sólo la
	 * elimina de la cola.
	 * 
	 * @return La petición. Null si no hay peticiones en cola.
	 */
	public ExecutionRequest next() {
		if (queue.isEmpty())
			return null;
		ExecutionRequest er = queue.remove();
		running = er;
		return er;
	}

	/**
	 * Vacía la cola de peticiones.
	 */
	public void clear() {
		queue.clear();
	}

	/**
	 * (Sólo para herramientas de datos). Elimina de la cola al usuario
	 * introducido.
	 * 
	 * @param user
	 *            Usuario.
	 */
	public void clearDataTool(User user) {

		if (!this.tool.isDataTool())
			return;

		LinkedList<ExecutionRequest> remove = new LinkedList<ExecutionRequest>();

		for (ExecutionRequest e : queue) {
			if (e.getUser() == null)
				continue;
			e.removeUser(user);
			if (e.getUser().equals(user)) {
				if (!e.changeOwner())
					remove.add(e);
			}
		}

		for (ExecutionRequest e : remove) {
			queue.remove(e);
		}
	}

	/**
	 * Elimina la petición en ejecución.
	 * 
	 * @return Petición eliminada.
	 */
	public ExecutionRequest finish() {
		ExecutionRequest ex = running;
		running = null;
		return ex;
	}

}
