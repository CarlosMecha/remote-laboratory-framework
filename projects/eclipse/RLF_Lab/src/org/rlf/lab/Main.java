/**
 * Main del demonio.
 */
package org.rlf.lab;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import org.rlf.lab.data.DBHelper;
import org.rlf.lab.tool.ToolManager;
import org.rlf.log.RLF_Log;

/**
 * Ejecución principal del demonio.
 * 
 * @author Carlos A. Rodriguez Mecha
 * @version 0.1
 * 
 */
public class Main {

	/**
	 * Método principal.
	 * 
	 * @param args
	 *            Argumentos de ejecución. (No se usan)
	 */
	public static void main(String[] args) {

		String name = null, provider = null;
		int labManagerPort = LabContext.DFL_LABMANAGER_PORT;
		int providerRequestPort = LabContext.DFL_PROVIDER_REQUEST_PORT;
		int providerPort = LabContext.DFL_PROVIDER_PORT;
		int clientPort = LabContext.DFL_CLIENT_PORT;
		int notificationPort = LabContext.DFL_NOTIFICATION_PORT;
		int max_process = LabContext.PROCESSORS;

		Properties prop = new Properties();
		FileInputStream in;

		try {

			in = new FileInputStream(System.getProperty("user.dir", "")
					+ File.separator + "res" + File.separator + "lab.conf");
			prop.load(in);
			in.close();

		} catch (IOException e) {
			RLF_Log.LabLog()
					.severe("[STARTLAB] El fichero de configuración no se encuentra o tiene un formato incorrecto.");
			return;
		}

		RLF_Log.LabLog().info("Lab iniciado " + (new Date()).toString());

		if ((name = prop.getProperty("lab_name")) == null) {
			RLF_Log.LabLog()
					.severe("[STARTLAB] En el fichero de configuración no se encuentra el nombre del laboratorio.");
			return;
		}

		if ((Kernel.Provider_User = prop.getProperty("user")) == null) {
			RLF_Log.LabLog()
					.severe("[STARTLAB] En el fichero de configuración no se encuentra el nombre del usuario.");
			return;
		}

		if ((Kernel.Provider_Pass = prop.getProperty("pass")) == null) {
			RLF_Log.LabLog()
					.severe("[STARTLAB] En el fichero de configuración no se encuentra la contraseña.");
			return;
		}

		if ((provider = prop.getProperty("provider_host")) == null) {
			RLF_Log.LabLog()
					.severe("[STARTLAB] En el fichero de configuración no se especifica la localización del servidor.");
			return;
		}

		try {
			providerPort = Integer.parseInt(prop.getProperty("provider_port"));
			if (providerPort <= 0)
				throw new Exception();

		} catch (Exception e) {
			RLF_Log.LabLog().warning(
					"[STARTLAB] El puerto de conexión con el proveedor será por defecto "
							+ LabContext.DFL_PROVIDER_PORT);
			providerPort = LabContext.DFL_PROVIDER_PORT;
		}

		try {
			labManagerPort = Integer.parseInt(prop
					.getProperty("labmanager_request_port"));
			if (labManagerPort <= 0)
				throw new Exception();

		} catch (Exception e) {
			RLF_Log.LabLog().warning(
					"[STARTLAB] El puerto de conexión con el labmanager será por defecto "
							+ LabContext.DFL_LABMANAGER_PORT);
			labManagerPort = LabContext.DFL_LABMANAGER_PORT;
		}

		try {
			clientPort = Integer.parseInt(prop
					.getProperty("client_request_port"));
			if (clientPort <= 0)
				throw new Exception();

		} catch (Exception e) {
			RLF_Log.LabLog().warning(
					"[STARTLAB] El puerto de conexión con el cliente será por defecto "
							+ LabContext.DFL_CLIENT_PORT);
			clientPort = LabContext.DFL_CLIENT_PORT;
		}

		try {
			notificationPort = Integer.parseInt(prop
					.getProperty("client_notification_port"));
			if (notificationPort <= 0)
				throw new Exception();

		} catch (Exception e) {
			RLF_Log.LabLog()
					.warning(
							"[STARTLAB] El puerto de conexión con el cliente para notificaciones será por defecto "
									+ LabContext.DFL_NOTIFICATION_PORT);
			notificationPort = LabContext.DFL_NOTIFICATION_PORT;
		}

		try {
			providerRequestPort = Integer.parseInt(prop
					.getProperty("provider_request_port"));
			if (providerRequestPort <= 0)
				throw new Exception();

		} catch (Exception e) {
			RLF_Log.LabLog()
					.warning(
							"[STARTLAB] El puerto de conexión con el proveedor para la recepción de peticiones será por defecto "
									+ LabContext.DFL_PROVIDER_REQUEST_PORT);
			providerRequestPort = LabContext.DFL_PROVIDER_REQUEST_PORT;
		}
		
		try {
			max_process = Integer.parseInt(prop
					.getProperty("max_process"));
			if (max_process <= 0)
				throw new Exception();

		} catch (Exception e) {
			RLF_Log.LabLog()
					.warning(
							"[STARTLAB] El número máximo de procesos en ejecución será por defecto "
									+ LabContext.PROCESSORS);
			max_process = LabContext.PROCESSORS;
		}

		// 2. Preparación de la ejecución.
		LabContext context = LabContext.getInstance(name, labManagerPort,
				providerRequestPort, provider, providerPort, clientPort,
				notificationPort, max_process);
		ToolManager tools = ToolManager.Instance(context);
		context.setToolManager(tools);
		Kernel kernel = Kernel.Instance(context);
		context.setKernel(kernel);

		// 3. Si no existe la base de datos se crea.
		DBHelper db = new DBHelper();
		File dbfile = new File(System.getProperty("user.dir", "")
				+ File.separator + "res" + File.separator + DBHelper.LABDBNAME);

		if (!dbfile.exists() || !dbfile.isFile()) {
			db.createDB(null);
			RLF_Log.LabLog()
					.warning(
							"[STARTLAB] La base de datos del laboratorio no ha sido encontrada, se creará una nueva.");
		}

		try {
			// 4. Ejecución.
			kernel.start();
			kernel.join();
		} catch (InterruptedException ie) {
			RLF_Log.LabLog().severe("[EXCEPTION] Hilo principal interrumpido");
		}

		// 5. Fin de la ejecución.
		RLF_Log.LabLog().info("Fin de la ejecución " + (new Date()).toString());

	}

}
