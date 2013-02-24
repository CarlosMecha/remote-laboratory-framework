/**
 * Información de ejecución.
 */
package org.rlf.lab;

import org.rlf.lab.connection.ConnectionManager;
import org.rlf.lab.tool.ToolManager;
import org.rlf.lab.tool.runtime.RuntimeManager;

/**
 * Contexto de ejecución del box.
 * 
 * @author rodriguezmecha
 * 
 */
public class LabContext {

	// Constantes:
	/** Puerto por defecto de acceso al laboratorio por el gestor remoto. */
	public final static int DFL_LABMANAGER_PORT = 6400;
	/** Puerto por defecto de acceso al laboratorio por el proveedor. */
	public final static int DFL_PROVIDER_REQUEST_PORT = 6401;
	/** Puerto por defecto de acceso al laboratorio por los clientes. */
	public final static int DFL_CLIENT_PORT = 6402;
	/** Puerto por defecto de notificación a los clientes. */
	public final static int DFL_NOTIFICATION_PORT = 6403;
	/** Puerto por defecto de acceso al proveedor. */
	public final static int DFL_PROVIDER_PORT = 3306;
	/** Número máximo de acciones en ejecución. */
	public final static int PROCESSORS = 4;

	// Atributos:
	/** Nombre del laboratorio. */
	private String name;

	/** Puerto de control del laboratorio mediante el gestor remoto. */
	private int labManagerPort;
	/** Puerto de control del laboratorio mediante el proveedor. */
	private int providerRequestPort;
	/** Localización del proveedor. */
	private String provider;
	/** Puerto de acceso al proveedor. */
	private int providerPort;
	/** Puerto de comunicación con los clientes. */
	private int clientPort;
	/** Puerto de notificación para los clientes. */
	private int notificationPort;
	/** Número máximo de procesos en ejecución. */
	private int processors;
	/** Indica si el laboratorio está activado. */
	private boolean armed;
	/** Señal de parada. */
	private boolean stopped;

	/** Hilo principal del laboratorio. */
	private Kernel kernel;
	/** Gestor de herramientas. */
	private ToolManager toolManager;
	/** Gestor de ejecución de acciones. */
	private RuntimeManager runtimeManager;
	/** Gestor de conexiones con los clientes. */
	private ConnectionManager connectionManager;

	/** Instancia del contexto. */
	private static LabContext instance = null;

	// Constructor:
	/**
	 * Constructor por defecto.
	 * 
	 * @param name
	 *            Nombre dado al laboratorio.
	 * @param server
	 *            Localización del servidor.
	 */
	private LabContext(String name, String server) {
		this.name = name;

		this.labManagerPort = DFL_LABMANAGER_PORT;
		this.providerRequestPort = DFL_PROVIDER_REQUEST_PORT;
		this.clientPort = DFL_CLIENT_PORT;
		this.notificationPort = DFL_NOTIFICATION_PORT;
		this.provider = server;
		this.providerPort = DFL_PROVIDER_PORT;
		this.processors = PROCESSORS;

		this.armed = false;
		this.stopped = false;

		this.kernel = null;
		this.toolManager = null;
		this.runtimeManager = null;
		this.connectionManager = null;

	}

	/**
	 * Constructor del contexto.
	 * 
	 * @param name
	 *            Nombre dado al laboratorio.
	 * @param labManagerPort
	 *            Puerto de acceso al laboratorio por el gestor remoto.
	 * @param providerRequestPort
	 *            Puerto de acceso al laboratorio por el proveedor.
	 * @param provider
	 *            Localización del proveedor.
	 * @param providerPort
	 *            Puerto de acceso al proveedor.
	 * @param clientPort
	 *            Puerto de comunicación con los clientes.
	 * @param notificationPort
	 *            Puerto de notificación para los clientes.
	 * @param processors
	 *            Número máximo de procesos en ejecución.
	 */
	private LabContext(String name, int labManagerPort,
			int providerRequestPort, String provider, int providerPort,
			int clientPort, int notificationPort, int processors) {
		this.name = name;

		this.labManagerPort = labManagerPort;
		this.providerRequestPort = providerRequestPort;
		this.provider = provider;
		this.providerPort = providerPort;
		this.clientPort = clientPort;
		this.notificationPort = notificationPort;
		this.processors = processors;

		this.armed = false;
		this.stopped = false;

		this.kernel = null;
		this.toolManager = null;
		this.runtimeManager = null;
		this.connectionManager = null;

	}

	/**
	 * Obtiene la instancia actual del contexto de ejecución. Los parámetros
	 * sólo se utilizan si es la primera vez que se llama a este método.
	 * 
	 * @param name
	 *            Nombre dado al laboratorio.
	 * @param provider
	 *            Localización del proveedor. IP o nombre.
	 * @return Contexto actual.
	 */
	public static LabContext Instance(String name, String provider) {
		if (LabContext.instance == null) {
			LabContext.instance = new LabContext(name, provider);
		}
		return instance;
	}

	/**
	 * Obtiene la instancia actual del contexto del laboratorios. Los parámetros
	 * sólo se utilizan si es la primera vez que se llama a este método.
	 * 
	 * @param name
	 *            Nombre dado al laboratorio.
	 * @param labManagerPort
	 *            Puerto de acceso al laboratorio por el gestor remoto.
	 * @param providerRequestPort
	 *            Puerto de acceso al laboratorio por el proveedor.
	 * @param provider
	 *            Localización del proveedor.
	 * @param providerPort
	 *            Puerto de acceso al proveedor.
	 * @param clientPort
	 *            Puerto de comunicación con los clientes.
	 * @param notificationPort
	 *            Puerto de notificación para los clientes.
	 * @param processors
	 *            Número máximo de procesos en ejecución.
	 * @return Contexto actual.
	 */
	public static LabContext getInstance(String name, int labManagerPort,
			int providerRequestPort, String provider, int providerPort,
			int clientPort, int notificationPort, int processors) {
		if (LabContext.instance == null) {
			LabContext.instance = new LabContext(name, labManagerPort,
					providerRequestPort, provider, providerPort, clientPort,
					notificationPort, processors);
		}
		return instance;
	}

	// Métodos getters:
	/**
	 * Obtiene el número máximo de procesos en ejecución.
	 * 
	 * @return Número.
	 */
	public int getProcessors(){
		return this.processors;
	}
	
	/**
	 * Obtiene el nombre asignado al laboratorio.
	 * 
	 * @return Nombre.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Obtiene el puerto de acceso al laboratorio por el gestor remoto.
	 * 
	 * @return Número de puerto.
	 */
	public int getLabManagerPort() {
		return this.labManagerPort;
	}

	/**
	 * Obtiene el puerto de acceso al laboratorio por el proveedor.
	 * 
	 * @return Número de puerto.
	 */
	public int getProviderRequestPort() {
		return this.providerRequestPort;
	}

	/**
	 * Localización del proveedor.
	 * 
	 * @return IP o nombre de la máquina del proveedor.
	 */
	public String getProvider() {
		return this.provider;
	}

	/**
	 * Obtiene el puerto de acceso al proveedor.
	 * 
	 * @return Número de puerto.
	 */
	public int getProviderPort() {
		return this.providerPort;
	}

	/**
	 * Obtiene el puerto para la comunicación con los clientes.
	 * 
	 * @return Número de puerto.
	 */
	public int getClientPort() {
		return this.clientPort;
	}

	/**
	 * Obtiene el puerto para notificar las acciones a los clientes.
	 * 
	 * @return Número de puerto.
	 */
	public int getNotificationPort() {
		return this.notificationPort;
	}

	/**
	 * Indica si el laboratorio ha sido armado.
	 * 
	 * @return Verdadero si ha sido armado.
	 */
	public synchronized boolean isArmed() {
		return this.armed;
	}

	/**
	 * Indica si el laboratorio está en proceso de parada.
	 * 
	 * @return Verdadero si es este estado.
	 */
	public synchronized boolean isStopped() {
		return this.stopped;
	}

	/**
	 * Obtiene el hilo principal del laboratorio.
	 * 
	 * @return Hilo principal en ejecución.
	 */
	public Kernel getKernel() {
		return this.kernel;
	}

	/**
	 * Obtiene el gestor de herramientas del laboratorio.
	 * 
	 * @return Gestor.
	 */
	public ToolManager getToolManager() {
		return this.toolManager;
	}

	/**
	 * Obtiene el gestor de ejecución de acciones.
	 * 
	 * @return Gestor.
	 */
	public RuntimeManager getRuntimeManager() {
		return this.runtimeManager;
	}

	/**
	 * Obtiene el gestor de conexiones con los clientes.
	 * 
	 * @return Gestor.
	 */
	public ConnectionManager getConnectionManager() {
		return this.connectionManager;
	}

	// Métodos setters:

	/**
	 * Cambia el estado de activación. No realiza la acción propia.
	 * 
	 * @param armed
	 *            Estado de activación.
	 */
	public synchronized void setArmed(boolean armed) {
		this.armed = armed;
	}

	/**
	 * Se realiza una parada del laboratorio.
	 */
	public synchronized void stop() {
		this.stopped = true;
	}

	/**
	 * Establece el hilo principal del laboratorio.
	 * 
	 * @param kernel
	 *            Hilo principal.
	 */
	public void setKernel(Kernel kernel) {
		this.kernel = kernel;
	}

	/**
	 * Establece el gestor de herramientas.
	 * 
	 * @param manager
	 *            Gestor.
	 */
	public void setToolManager(ToolManager manager) {
		this.toolManager = manager;
	}

	/**
	 * Establece el gestor de ejecución de acciones.
	 * 
	 * @param manager
	 *            Gestor.
	 */
	public void setRuntimeManager(RuntimeManager manager) {
		this.runtimeManager = manager;
	}

	/**
	 * Establece el gestor de comunicaciones con los clientes.
	 * 
	 * @param manager
	 *            Gestor.
	 */
	public void setConnectionManager(ConnectionManager manager) {
		this.connectionManager = manager;
	}

	// Métodos varios:
	/**
	 * Vuelve al estado incial el contexto del laboratorio. Desarmado y sin
	 * estados de parada. Mantiene el nombre y los parámetros de conexión.
	 * También elimina los managers pero no al hilo principal.
	 */
	public void reset() {

		this.armed = false;
		this.stopped = false;

		this.toolManager = null;
		this.runtimeManager = null;
		this.connectionManager = null;

	}

}
