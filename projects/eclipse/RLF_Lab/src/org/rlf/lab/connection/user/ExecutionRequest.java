/**
 * Petición de ejecución.
 */
package org.rlf.lab.connection.user;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Representa a una petición realizada por el usuario para ejecutar una acción
 * concreta de una herramienta.
 * 
 * @author Carlos A. Rodriguez Mecha
 * @version 0.1
 */
public class ExecutionRequest {

	// Enum:
	/** Estados de la petición. */
	public enum RequestStatus {
		/** En espera de ejecutar. */
		QUEUE,
		/** Ejecutado con éxito. */
		FINISH,
		/**
		 * Parado por exceder el tiempo máximo de ejecución o con problemas con
		 * la base de datos.
		 */
		ERROR;
	};

	// Atributos:
	/** Identificador de la herramienta asociada. */
	private int idTool;
	/** Nombre de la acción. */
	private String action;
	/** Usuario de la petición. */
	private User user;
	/**
	 * Cambios a realizar en la base de datos antes de ejecutar la acción. La
	 * clave representa el nombre del parámetro. Cuando se ha ejecutado la
	 * acción, son los parámetros que se han modificado, así como el estado y
	 * las excepciones.
	 */
	private HashMap<String, String> changes;
	/**
	 * (Sólo para herramientas de datos). Almacena el número de usuarios que
	 * están utilizando o a la espera de la acción.
	 */
	private LinkedList<User> users;
	/** Estado de la petición. */
	private RequestStatus status;

	// Constructor:
	/**
	 * Constructor de una petición.
	 * 
	 * @param idTool
	 *            Identificador de la herramienta.
	 * @param action
	 *            Nombre de la acción.
	 * @param user
	 *            Usuario de la petición. Si es null es el propio Lab.
	 * @param changes
	 *            Cambios que hay que realizar en la base de datos. Puede ser
	 *            null.
	 */
	public ExecutionRequest(int idTool, String action, User user,
			HashMap<String, String> changes) {
		this.idTool = idTool;
		this.action = action;
		this.user = user;
		this.changes = changes;
		this.users = new LinkedList<User>();
		this.status = RequestStatus.QUEUE;
	}

	// Métodos getters y setters:
	/**
	 * Obtiene el identificador de la herramienta asociada a la petición.
	 * 
	 * @return Identificador.
	 */
	public int getIdTool() {
		return idTool;
	}

	/**
	 * Nombre de la acción asociada a la petición.
	 * 
	 * @return Nombre.
	 */
	public String getActionName() {
		return action;
	}

	/**
	 * Usuario que realiza la petición en origen. Si es una herramienta de datos
	 * puede tener varios en el otro atributo.
	 * 
	 * @return Usuario de la petición. Si es null es el propio Lab.
	 */
	public User getUser() {
		return user;
	}

	/**
	 * Cambios que se realizan antes de ejecutar la acción. Después es el objeto
	 * que representa los cambios que se han realizado después de la ejecución.
	 * 
	 * @return Cambios de la base de datos.
	 */
	public HashMap<String, String> getChanges() {
		return changes;
	}

	/**
	 * Obtiene el estado actual de la petición.
	 * 
	 * @return Estado.
	 */
	public RequestStatus getStatus() {
		return status;
	}

	/**
	 * Obtiene todos los usuarios que han realizado la petición de la
	 * herramienta de datos.
	 * 
	 * @return Usuarios.
	 */
	public LinkedList<User> getUsers() {
		return this.users;
	}

	/**
	 * Cambia el estado a finalizado.
	 */
	public void finish() {
		this.status = RequestStatus.FINISH;
	}

	/**
	 * Cambia el estado a finalizado con errores.
	 */
	public void error() {
		this.status = RequestStatus.ERROR;
	}

	/**
	 * Sustituye los cambios realizados en la base de datos.
	 * 
	 * @param changes
	 *            Cambios.
	 */
	public void setChanges(HashMap<String, String> changes) {
		this.changes = changes;
	}

	// Métodos varios:
	/**
	 * (Sólo para herramientas de datos). Añade un usuario a la petición.
	 * 
	 * @param user
	 *            Usuario.
	 * @return Indica si se ha podido introducir el usuario.
	 */
	public boolean addUser(User user) {
		return users.add(user);
	}

	/**
	 * (Sólo para herramientas de datos). Elimina al usuario introducido. Si es
	 * el dueño intenta hacer un cambio de usuario.
	 * 
	 * @param user
	 *            Usuario a eliminar.
	 * @return Verdadero si ha podido eliminar el usuario. Falso si no hay más a
	 *         la espera, o si no contiene a ese usuario.
	 */
	public boolean removeUser(User user) {

		boolean r = users.remove(user);

		if ((this.user != null) && (this.user.equals(user))) {
			return changeOwner();
		}
		return r;

	}

	/**
	 * (Sólo para herramientas de datos). Indica cuántos usuarios hay para esta
	 * petición.
	 * 
	 * @return Número de usuarios.
	 */
	public int users() {
		return users.size();
	}

	/**
	 * (Sólo para herramientas de datos). Sustituye el dueño de la petición por
	 * el primero de la lista si se puede.
	 * 
	 * @return Verdadero si ha podido hacerlo. Falso si la petición no tiene a
	 *         otros usuarios.
	 */
	public boolean changeOwner() {
		if (users.isEmpty())
			return false;
		user = users.remove();
		return true;
	}

	/**
	 * Indica si el usuario introducido es dueño de la petición o está en cola
	 * por ser una herramienta de datos.
	 * 
	 * @return Verdadero si está.
	 */
	public boolean containsUser(User user) {
		if ((this.user != null) && (this.user.equals(user)))
			return true;
		return this.users.contains(user);
	}

}
