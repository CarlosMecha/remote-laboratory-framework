/**
 * Gestor de logs.
 */
package org.rlf.log;

import java.io.File;
import java.util.logging.*;

/**
 * Gestor de los logs de RLF.
 * 
 * @author Carlos A. Rodriguez Mecha
 * @version 0.1
 */
public class RLF_Log {

	// Constantes:
	/** Directorio de logs. */
	public final static String LOG_DIR = System.getProperty("user.dir", "")
			+ File.separator + "log";

	/**
	 * Crea el directorio "log" si es necesario en la ruta del programa.
	 */
	private final static void MakeDir() {

		File dir = new File(LOG_DIR);

		if (!dir.exists() || !dir.isDirectory()) {
			dir.mkdir();
		}

	}

	/**
	 * Obtiene el log del proveedor.
	 * 
	 * @return Log.
	 */
	public final static Logger ProviderLog() {

		Logger logger = Logger.getLogger("ProviderLog");
		FileHandler fh;

		try {

			// 1. Configuración del log.
			MakeDir();
			fh = new FileHandler(LOG_DIR + File.separator + "Provider.log", true);
			logger.addHandler(fh);
			logger.setLevel(Level.ALL);
			SimpleFormatter formatter = new SimpleFormatter();
			fh.setFormatter(formatter);

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return logger;
	}

	/**
	 * Obtiene el log del laboratorio.
	 * 
	 * @return Log.
	 */
	public final static Logger LabLog() {

		Logger logger = Logger.getLogger("LabLog");
		FileHandler fh;

		try {

			// 1. Configuración del log.
			MakeDir();
			fh = new FileHandler(LOG_DIR + File.separator + "Lab.log", true);
			logger.addHandler(fh);
			logger.setLevel(Level.ALL);
			SimpleFormatter formatter = new SimpleFormatter();
			fh.setFormatter(formatter);

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return logger;
	}

	/**
	 * Obtiene el log general de RLF.
	 * 
	 * @return Log.
	 */
	public final static Logger Log() {

		Logger logger = Logger.getLogger("General");
		FileHandler fh;

		try {

			// 1. Configuración del log.
			MakeDir();
			fh = new FileHandler(LOG_DIR + File.separator + "RLF.log", true);
			logger.addHandler(fh);
			logger.setLevel(Level.ALL);
			SimpleFormatter formatter = new SimpleFormatter();
			fh.setFormatter(formatter);

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return logger;
	}

}
