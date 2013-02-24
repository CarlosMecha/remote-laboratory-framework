/**
 * Usuario del Lab.
 */
package org.rlf.lab.connection.user;

import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Timer;

/**
 * Usuario válido que puede realizar peticiones al laboratorio.
 * 
 * @author Carlos A. Rodriguez Mecha
 * @version 0.1
 */
public class User {

	// Atributos:
	/** Token de uso del usuario. */
	private String token;
	/** Tiempo máximo de conexión en minutos. */
	private int timeout;
	/** Cronómetro de tiempo. */
	private Timer timer;
	/** Puerto asignado al cliente para enviarle notificaciones. */
	private SocketChannel notificationSocket;
	/** Puerto de escucha de peticiones. */
	private SocketChannel requestSocket;

	/**
	 * Lista identificadores de las herramientas a las que el usuario tiene
	 * acceso.
	 */
	private LinkedList<Integer> tools;

	// Constructor:
	/**
	 * Constructor del usuario.
	 * 
	 * @param token
	 *            Token de uso.
	 * @param timeout
	 *            Tiempo máximo de conexión en minutos.
	 * @param tools
	 *            Lista identificadores de las herramientas a las que el usuario
	 *            tiene acceso.
	 * @param timer
	 *            Cronómetro asignado al usuario.
	 */
	public User(String token, int timeout, LinkedList<Integer> tools,
			Timer timer) {
		this.token = token;
		this.timeout = timeout;
		this.tools = tools;
		this.timer = timer;
		this.notificationSocket = null;
		this.requestSocket = null;

	}

	// Métodos getters:
	/**
	 * Token de uso válido para ese usuario. Este lo enviará cada vez que quiera
	 * realizar una acción.
	 * 
	 * @return El token de acceso.
	 */
	public String getToken() {
		return token;
	}

	/**
	 * Obtiene el socket asignado al usuario para que envíe peticiones de
	 * ejecución. Puede valer null si aún no se ha establecido un socket o ha
	 * sido cerrado y eliminado.
	 * 
	 * @return Socket conectado.
	 */
	public SocketChannel getRequestSocket() {
		return requestSocket;
	}

	/**
	 * Obtiene el socket asignado al usuario para notificarle las acciones y
	 * procesos ejecutados. Puede valer null si aún no se ha establecido un
	 * socket o ha sido cerrado y eliminado.
	 * 
	 * @return Socket conectado.
	 */
	public SocketChannel getNotificationSocket() {
		return notificationSocket;
	}

	/**
	 * Tiempo máximo en minutos que tiene el usuario para usar el laboratorio.
	 * 
	 * @return Tiempo máximo de conexión.
	 */
	public int getTimeout() {
		return timeout;
	}

	// Métodos setters:
	/**
	 * Asigna el socket como canal para la recepción de peticiones. Debe estar
	 * abierto.
	 * 
	 * @param socket
	 *            Socket de envío de peticiones.
	 */
	public void setRequestSocket(SocketChannel socket) {
		this.requestSocket = socket;
	}

	/**
	 * Asigna el socket como canal para el envío al usuario de notificaciones.
	 * Debe estar abierto.
	 * 
	 * @param socket
	 *            Socket de notificaciones.
	 */
	public void setNotificationSocket(SocketChannel socket) {
		this.notificationSocket = socket;
	}

	// Métodos varios:
	/**
	 * Indica si un usuario tiene acceso a una herramienta concreta.
	 * 
	 * @param id
	 *            Identificador de la herramienta.
	 * @return Verdadero si puede usarla.
	 */
	public boolean canUse(int id) {
		return this.tools.contains(id);
	}

	/**
	 * Para el cronómetro asignado por desconexión. Sólo tiene efecto si el
	 * cronómetro estaba en marcha.
	 */
	public void stopTimer() {
		this.timer.cancel();
		this.timer.purge();
	}

	/**
	 * Obtiene el hash del objeto. Se ha modificado para obtener el hash sólo
	 * del token de uso, y que en la evaluación del método equals se defina que
	 * dos usuarios son el mismo si tiene el mismo token de uso.
	 * 
	 * @return Código hash.
	 */
	@Override
	public int hashCode() {
		return this.token.hashCode();
	}
}
