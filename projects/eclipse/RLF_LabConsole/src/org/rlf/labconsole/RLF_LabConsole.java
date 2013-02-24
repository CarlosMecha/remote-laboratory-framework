/**
 * Aplicación de consola para gestionar el laboratorio.
 */
package org.rlf.labconsole;

import java.util.HashMap;
import java.util.Map.Entry;

import org.rlf.labmanager.LabManager;

/**
 * Aplicación simple de consola que gestiona el laboratorio mediante la
 * utilización de la librería LabManager.
 * 
 * @author Carlos A. Rodriguez Mecha
 * @version 0.1
 */
public class RLF_LabConsole {

	/**
	 * Cabecera de la aplicación. Imprime la cabecera de la aplicación.
	 */
	protected static void Header() {
		
		System.out.println("RLF LabConsole 0.1");
	}

	/**
	 * Ayuda de la aplicación. Muestra por pantalla los parámetros a introducir.
	 */
	protected static void Help() {

		System.out
				.println("Usage: java -jar LabConsole.jar [-h <Lab IP or Hostname> "
						+ "-p <Lab port>] -user <user> -pass <pass> <command> <parameters>");
		System.out.println("       java LabConsole.jar --help");
		System.out
				.println("<command>: (arm | disarm | registry | drop | status | emergency | stop)");
		System.out
				.println("- arm: Activa el laboratorio. Debe estar el demonio ejecutándose.");
		System.out.println("- - <parameters>: empty");
		System.out.println("- disarm: Desactiva el laboratorio accedido.");
		System.out.println("- - <parameters>: empty");
		System.out
				.println("- registry: Registra una herramienta descrito en el fichero XML.");
		System.out.println("- - <parameters>: <path to XML File>");
		System.out.println("- drop: Elimina una herramienta del laboratorio.");
		System.out.println("- - <parameters>: <Service ID> <Service Key>");
		System.out.println("- status: Obtiene el estado del laboratorio.");
		System.out.println("- - <parameters>: empty");
		System.out
				.println("- emergency: Emite una señal de emergencia al laboratorio.");
		System.out.println("- - <parameters>: <Emergency Key>");
		System.out.println("- stop: Detiene por completo el laboratorio.");
		System.out.println("- - <parameters>: empty");

	}

	/**
	 * Método principal.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		String host = LabManager.DFL_LAB_HOST, admin = null, pass = null, arg1 = null, arg2 = null;
		int port = LabManager.DFL_LAB_PORT, op = -1, id = 0;
		LabManager labManager;

		// 1. Cabecera.
		Header();

		if (args.length == 0) {
			Help();
			System.exit(-1);
		}

		// 2. Argumentos.
		try {

			for (int i = 0; i < args.length; i++) {

				if (args[i].compareTo("--help") == 0) {
					Help();
					System.exit(0);
				} else if (args[i].compareTo("-h") == 0)
					host = args[++i];
				else if (args[i].compareTo("-p") == 0)
					port = Integer.parseInt(args[++i]);
				else if (args[i].compareTo("-user") == 0)
					admin = args[++i];
				else if (args[i].compareTo("-pass") == 0)
					pass = args[++i];
				else if (args[i].compareTo("arm") == 0) {
					op = 0;
					break;
				} else if (args[i].compareTo("disarm") == 0) {
					op = 1;
					break;
				} else if (args[i].compareTo("registry") == 0) {
					op = 2;
					arg1 = args[++i];
					break;
				} else if (args[i].compareTo("drop") == 0) {
					op = 3;
					arg1 = args[++i];
					arg2 = args[++i];
					break;
				} else if (args[i].compareTo("status") == 0) {
					op = 4;
					break;
				} else if (args[i].compareTo("emergency") == 0) {
					op = 5;
					arg1 = args[++i];
					break;
				} else if (args[i].compareTo("stop") == 0) {
					op = 6;
					break;
				} else {
					Help();
					System.exit(-1);
				}

			}

		} catch (Exception e) {
			Help();
			System.exit(-1);
		}

		if (admin == null | pass == null) {
			Help();
			System.exit(-1);
		}

		// 3. Conexión con el laboratorio.
		labManager = new LabManager(host, port, admin, pass);

		try {
			switch (op) {
			// Arm:
			case 0:
				System.out.println("Activating...");
				if (labManager.armLab())
					System.out.println("The lab is now activated.");
				else
					System.err.print("[ERROR] The Lab is already activated.");
				break;

			// Disarm:
			case 1:
				System.out.println("Deactivating...");
				if (labManager.disarmLab())
					System.out.println("The lab is disarmed.");
				else
					System.err.print("[ERROR] The lab is already disarmed.");
				break;

			// Registry:
			case 2:
				if (arg1 == null) {
					Help();
					System.exit(-1);
				}
				System.out.println("Registry service...");
				HashMap<Integer, String> map = labManager.registryTool(arg1);
				if (map == null)
					System.err
							.println("[ERROR] The tool is already registred, the lab is activated or de XML is invalid.");
				else {
					Entry<Integer, String> e = map.entrySet().iterator().next();
					System.out
							.println("Set this key in the initiation of service. (RLF_Init function):");
					System.out.println("Service id: " + e.getKey() + " KEY: "
							+ e.getValue());
					System.out.println("Service registred.");
				}
				break;

			// Drop:
			case 3:
				if (arg1 == null | arg2 == null) {
					Help();
					System.exit(-1);
				}

				try {
					id = Integer.parseInt(arg1);
				} catch (Exception ex) {
					System.err.println("The id must be a number.");
					break;
				}

				System.out.println("Drop service...");
				if (labManager.dropTool(id, arg2))
					System.out.println("Service (ID " + id + ") dropped.");
				else
					System.err
							.print("[ERROR] This tool doesn't exist or the lab is actived.");
				break;

			// Status:
			case 4:
				System.out.println("Lab status:");
				for (Entry<Integer, String> entry : labManager.labStatus()
						.entrySet()) {
					if (entry.getKey() == 0) {
						System.out.println("Lab: " + entry.getValue());
					} else
						System.out.println("Tool " + entry.getKey() + ": "
								+ entry.getValue());
				}
				break;

			// EmergencyStop:
			case 5:
				if (arg1 == null) {
					Help();
					System.exit(-1);
				}

				System.out.println("Sending emergency signal...");
				if (!labManager.emergencyStop(arg1)) {
					System.out.println("[ERROR] Access denied.");
				} else
					System.out.println("The lab is stopped.");
				break;

			// Stop:
			case 6:
				System.out.println("Stopping...");
				if (labManager.stopLab())
					System.out.println("The lab is now stopped.");
				else
					System.err.print("[ERROR] The lab can't stop.");
				break;

			default:
				Help();
				System.exit(-1);
			}
		} catch (Exception e) {
			System.err.println("[ERROR] " + e.getMessage());
			System.exit(-1);
		}

	}

}
