/**
 * Librería principal para un LabManager.
 */
package org.rlf.labmanager;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map.Entry;

import org.rlf.cipher.RLF_Cipher;
import org.rlf.cipher.dummy.RLF_DummyCipher;
import org.rlf.labmanager.exception.*;
import org.rlf.log.RLF_Log;
import org.rlf.net.RLF_NetHelper;
import org.rlf.net.message.RLF_NetMessage;
import org.rlf.net.message.RLF_NetMessageID;

/**
 * Gestor de un laboratorio, de forma remota o local. Puede armarlo, desarmarlo,
 * pararlo, notificar una parada de emergencia, registrar un servicio y
 * eliminarlo. Además de obtener el estado del propio laboratorio.
 * 
 * @author Carlos A. Rodriguez Mecha
 * @version 0.1
 * 
 */
public class LabManager {

	// Constantes:
	/** Token privado de autentificación. */
	private static String LABMANAGER_KEY = "Communication";
	/** Puerto por defecto del laboratorio. */
	public static int DFL_LAB_PORT = 6400;
	/** IP o nombre por defecto de la máquina del laboratorio. */
	public static String DFL_LAB_HOST = "127.0.0.1";

	// Atributos:
	/** Puerto de conexión del laboratorio. */
	private int labPort;
	/** Nombre o IP de la máquina del laboratorio. */
	private String labHost;
	/** Hash del administrador y constraseña. */
	private String hashAdmin;

	// Constructor:
	/**
	 * Constructor por defecto. Asigna los valores de conexión por defecto.
	 * 
	 * @param user
	 *            Administrador.
	 * @param pass
	 *            Password del administrador.
	 */
	public LabManager(String user, String pass) {

		this.labPort = DFL_LAB_PORT;
		this.labHost = DFL_LAB_HOST;
		RLF_Cipher cipher = new RLF_DummyCipher();
		this.hashAdmin = cipher.getHash(user + cipher.getHash(pass));
	}

	/**
	 * Constructor especificando todos los parámetros de conexión.
	 * 
	 * @param labHost
	 *            Nombre o IP del equipo del laboratorio.
	 * @param labPort
	 *            Puerto de conexión del laboratorio.
	 * @param user
	 *            Administrador.
	 * @param pass
	 *            Password del administrador.
	 */
	public LabManager(String labHost, int labPort, String user, String pass) {

		this.labPort = labPort;
		this.labHost = labHost;
		RLF_Cipher cipher = new RLF_DummyCipher();
		this.hashAdmin = cipher.getHash(user + cipher.getHash(pass));
	}

	// Métodos Getters y Setters:
	/**
	 * Obtiene el puerto de acceso al laboratorio.
	 * 
	 * @return Número de puerto.
	 */
	public int getLabPort() {
		return labPort;
	}

	/**
	 * Obtiene el nombre de la máquina del laboratorio.
	 * 
	 * @return Nombre o IP del equipo.
	 */
	public String getLabHost() {
		return labHost;
	}

	// Métodos varios:
	/**
	 * Conecta con el laboratorio asignado de forma bloqueante. Debe cerrarse
	 * con el método disconnect() o close() cuando se deje de utilizar.
	 * 
	 * @return Socket de conexión. Null si no ha podido establecerla.
	 */
	private SocketChannel connect() {

		SocketChannel socket = null;
		SocketAddress address = new InetSocketAddress(this.labHost,
				this.labPort);

		// 1. Se activa el socket.
		try {
			socket = SocketChannel.open(address);
			socket.configureBlocking(true); // Bloqueante.

		} catch (Exception e) {
			RLF_Log.Log().warning(
					"No se puede conectar con el laboratorio (" + this.labHost
							+ ":" + this.labPort + ")");
			return null;
		}

		return socket;
	}

	/**
	 * Desconectal el socket del laboratorio.
	 * 
	 * @param socket
	 *            Socket conectado con el método connect().
	 */
	private void disconnect(SocketChannel socket) {

		try {
			socket.close();
		} catch (Exception e) {
			RLF_Log.Log().warning("Error al desconectar con el laboratorio.");
		}
	}

	/**
	 * Activa el laboratorio. Aceptará peticiones de los clientes y podrá
	 * ejecutar las acciones.
	 * 
	 * @return Verdadero si se ha podido activar. Falso en caso contrario, puede
	 *         ser debido a que ya estuviera activado.
	 * @throws AuthException
	 *             La autentificación (del API o del administrador) no es
	 *             válida.
	 * @throws ConnectionException
	 *             No se ha podido conectar con el laboratorio.
	 */
	public boolean armLab() throws AuthException, ConnectionException {

		HashMap<String, String> attributes = new HashMap<String, String>();
		RLF_NetHelper net = new RLF_NetHelper();
		RLF_NetMessage msg = new RLF_NetMessage();

		// 1. Se introduce la operación y los atributos.
		msg.setId(RLF_NetMessageID.ARMLAB);
		attributes.put("key", LABMANAGER_KEY);
		attributes.put("hash", this.hashAdmin);
		msg.setAttributes(attributes);

		// 2. Se conecta el socket.
		SocketChannel socket = connect();
		if (socket == null)
			throw new ConnectionException();

		// 3. Se envía el mensaje y se espera a recibir.
		if (!net.sendMessage(msg, socket))
			throw new ConnectionException();
		msg = net.reciveMessage(socket);
		disconnect(socket);

		if (msg == null)
			throw new ConnectionException();
		else if (msg.getId() == RLF_NetMessageID.AUTHFAIL)
			throw new AuthException();
		else if (msg.getId() == RLF_NetMessageID.OK)
			return true;
		else
			return false;

	}

	/**
	 * Desactiva el laboratorio. Este estado es para insertar o eliminar
	 * servicios.
	 * 
	 * @return Verdadero si se ha podido desactivar. Falso en caso de que no
	 *         estuviera activo.
	 * @throws AuthException
	 *             La autentificación (del API o del administrador) no es
	 *             válida.
	 * @throws ConnectionException
	 *             No se ha podido conectar.
	 */
	public boolean disarmLab() throws AuthException, ConnectionException {

		HashMap<String, String> attributes = new HashMap<String, String>();
		RLF_NetHelper net = new RLF_NetHelper();
		RLF_NetMessage msg = new RLF_NetMessage();

		// 1. Se introduce la operación y los atributos.
		msg.setId(RLF_NetMessageID.DISARMLAB);
		attributes.put("key", LABMANAGER_KEY);
		attributes.put("hash", this.hashAdmin);
		msg.setAttributes(attributes);

		// 2. Se conecta el socket.
		SocketChannel socket = connect();
		if (socket == null)
			throw new ConnectionException();

		// 3. Se envía el mensaje y se espera a recibir.
		if (!net.sendMessage(msg, socket))
			throw new ConnectionException();
		msg = net.reciveMessage(socket);
		disconnect(socket);

		if (msg == null)
			throw new ConnectionException();
		else if (msg.getId() == RLF_NetMessageID.AUTHFAIL)
			throw new AuthException();
		else if (msg.getId() == RLF_NetMessageID.OK)
			return true;
		else
			return false;
	}

	/**
	 * Registra una herramienta en el laboratorio. Sólo podrá realizarse si el
	 * laboratorio no está armado.
	 * 
	 * @param path
	 *            Ruta (en la máquina del laboratorio) del fichero XML de la
	 *            herramienta.
	 * @return Identificador y clave propios de la herramienta. Null en caso de
	 *         que estuviera activo el laboratorio o de que ya exista ese
	 *         servicio registrado.
	 * @throws AuthException
	 *             La autentificación (del API o del administrador) no es
	 *             válida.
	 * @throws ConnectionException
	 *             No se ha podido conectar.
	 * @throws FormatException
	 *             La herramienta tenía un formato no válido.
	 */
	public HashMap<Integer, String> registryTool(String path)
			throws FormatException, AuthException, ConnectionException {

		HashMap<String, String> attributes = new HashMap<String, String>();
		HashMap<Integer, String> reply = new HashMap<Integer, String>();
		Entry<String, String> entry;
		RLF_NetHelper net = new RLF_NetHelper();
		RLF_NetMessage msg = new RLF_NetMessage();

		// 1. Se introduce la operación y los atributos.
		msg.setId(RLF_NetMessageID.REGISTRYTOOL);
		attributes.put("key", LABMANAGER_KEY);
		attributes.put("hash", this.hashAdmin);
		attributes.put("path", path);
		msg.setAttributes(attributes);

		// 2. Se conecta el socket.
		SocketChannel socket = connect();
		if (socket == null)
			throw new ConnectionException();

		// 3. Se envía el mensaje y se espera a recibir.
		if (!net.sendMessage(msg, socket)) {
			disconnect(socket);
			throw new ConnectionException();
		}
		msg = net.reciveMessage(socket);
		disconnect(socket);

		if (msg == null)
			throw new ConnectionException();
		else if (msg.getId() == RLF_NetMessageID.AUTHFAIL)
			throw new AuthException();
		else if (msg.getId() == RLF_NetMessageID.FORMATERROR)
			throw new FormatException();
		else if (msg.getId() == RLF_NetMessageID.TOOLKEY) {
			entry = msg.getAttributes().entrySet().iterator().next();
			reply.put(Integer.parseInt(entry.getKey()), entry.getValue());
			return reply;
		} else
			return null;
	}

	/**
	 * Elimina una herramienta ya registrada en el laboratorio.
	 * 
	 * @param id
	 *            Identificador de la herramienta.
	 * @param key
	 *            Clave de la herramienta..
	 * @return Verdadero si se ha podido eliminar. Falso en caso de que no
	 *         exista esa herramienta o que el laboratorio siga aún activo.
	 * @throws AuthException
	 *             La autentificación (del API, del administrador o del
	 *             servicio) no es válida.
	 * @throws ConnectionException
	 *             No se ha podido conectar.
	 */
	public boolean dropTool(int id, String key) throws AuthException,
			ConnectionException {

		HashMap<String, String> attributes = new HashMap<String, String>();
		RLF_NetHelper net = new RLF_NetHelper();
		RLF_NetMessage msg = new RLF_NetMessage();

		// 1. Se introduce la operación y los atributos.
		msg.setId(RLF_NetMessageID.DROPTOOL);
		attributes.put("key", LABMANAGER_KEY);
		attributes.put("hash", this.hashAdmin);
		attributes.put("id", new Integer(id).toString());
		attributes.put("toolkey", key);
		msg.setAttributes(attributes);

		// 2. Se conecta el socket.
		SocketChannel socket = connect();
		if (socket == null)
			throw new ConnectionException();

		// 3. Se envía el mensaje y se espera a recibir.
		if (!net.sendMessage(msg, socket)) {
			disconnect(socket);
			throw new ConnectionException();
		}
		msg = net.reciveMessage(socket);
		disconnect(socket);

		if (msg == null)
			throw new ConnectionException();
		else if (msg.getId() == RLF_NetMessageID.AUTHFAIL)
			throw new AuthException();
		else if (msg.getId() == RLF_NetMessageID.OK)
			return true;
		else
			return false;
	}

	/**
	 * Obtiene el estado del laboratorio y de sus herramientas.
	 * 
	 * @return Lista con las distintas herramientas. y su estado. El
	 *         identificador 0 corresponde al propio Laboratorio.
	 * @throws AuthException
	 *             La autentificación (del API o del administrador) no es
	 *             válida.
	 * @throws ConnectionException
	 *             No se ha podido conectar.
	 */
	public HashMap<Integer, String> labStatus() throws AuthException,
			ConnectionException {

		HashMap<String, String> attributes = new HashMap<String, String>();
		HashMap<Integer, String> map = new HashMap<Integer, String>();
		RLF_NetHelper net = new RLF_NetHelper();
		RLF_NetMessage msg = new RLF_NetMessage();

		// 1. Se introduce la operación y los atributos.
		msg.setId(RLF_NetMessageID.LABSTATUS);
		attributes.put("key", LABMANAGER_KEY);
		attributes.put("hash", this.hashAdmin);
		msg.setAttributes(attributes);

		// 2. Se conecta el socket.
		SocketChannel socket = connect();
		if (socket == null)
			throw new ConnectionException();

		// 3. Se envía el mensaje y se espera a recibir.
		if (!net.sendMessage(msg, socket)) {
			disconnect(socket);
			throw new ConnectionException();
		}
			
		msg = net.reciveMessage(socket);
		disconnect(socket);

		if (msg == null)
			throw new ConnectionException();
		else if (msg.getId() == RLF_NetMessageID.AUTHFAIL)
			throw new AuthException();
		for (Entry<String, String> e : msg.getAttributes().entrySet()) {
			try {
				map.put(Integer.parseInt(e.getKey()), e.getValue());
			} catch (Exception ex) {
			}
		}

		return map;
	}

	/**
	 * Notifica una parada de emergencia al laboratorio. Parará todos los
	 * servicios en ejecución y se desconectará.
	 * 
	 * @param key
	 *            Clave de emergencia.
	 * @return Verdadero si se ha enviado correctamente.
	 * @throws AuthException
	 *             La clave de emergencia, la clave del api o la utentificación
	 *             del administrador no son válidas.
	 * @throws ConnectionException
	 *             No se ha podido conectar.
	 */
	public boolean emergencyStop(String key) throws AuthException,
			ConnectionException {

		HashMap<String, String> attributes = new HashMap<String, String>();
		RLF_NetHelper net = new RLF_NetHelper();
		RLF_NetMessage msg = new RLF_NetMessage();

		// 1. Se introduce la operación y los atributos.
		msg.setId(RLF_NetMessageID.EMERGENCYSTOP);
		attributes.put("key", LABMANAGER_KEY);
		attributes.put("hash", this.hashAdmin);
		attributes.put("!", key);
		msg.setAttributes(attributes);

		// 2. Se conecta el socket.
		SocketChannel socket = connect();
		if (socket == null)
			throw new ConnectionException();

		// 3. Se envía el mensaje y se espera a recibir.
		if (!net.sendMessage(msg, socket)) {
			disconnect(socket);
			throw new ConnectionException();
		}
		msg = net.reciveMessage(socket);
		disconnect(socket);

		if (msg == null)
			throw new ConnectionException();
		else if (msg.getId() == RLF_NetMessageID.AUTHFAIL)
			throw new AuthException();
		else if (msg.getId() == RLF_NetMessageID.OK)
			return true;
		else
			return false;
	}

	/**
	 * Inicia el proceso de una parada total del laboratorio.
	 * 
	 * @return Verdadero si se ha enviado correctamente.
	 * @throws AuthException
	 *             La autentificación (del API o del administrador) no es
	 *             válida.
	 * @throws ConnectionException
	 *             No se ha podido conectar.
	 */
	public boolean stopLab() throws AuthException, ConnectionException {

		HashMap<String, String> attributes = new HashMap<String, String>();
		RLF_NetHelper net = new RLF_NetHelper();
		RLF_NetMessage msg = new RLF_NetMessage();

		// 1. Se introduce la operación y los atributos.
		msg.setId(RLF_NetMessageID.STOPLAB);
		attributes.put("key", LABMANAGER_KEY);
		attributes.put("hash", this.hashAdmin);
		msg.setAttributes(attributes);

		// 2. Se conecta el socket.
		SocketChannel socket = connect();
		if (socket == null)
			throw new ConnectionException();

		// 3. Se envía el mensaje y se espera a recibir.
		if (!net.sendMessage(msg, socket)) {
			disconnect(socket);
			throw new ConnectionException();
		}
		msg = net.reciveMessage(socket);
		disconnect(socket);

		if (msg == null)
			throw new ConnectionException();
		else if (msg.getId() == RLF_NetMessageID.AUTHFAIL)
			throw new AuthException();
		else if (msg.getId() == RLF_NetMessageID.OK)
			return true;
		else
			return false;
	}

}
