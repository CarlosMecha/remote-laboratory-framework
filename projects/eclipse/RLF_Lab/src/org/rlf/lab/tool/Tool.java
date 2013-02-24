/**
 * Representa una herramienta activa en el laboratorio.
 */
package org.rlf.lab.tool;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.rlf.lab.data.DBHelper;
import org.rlf.lab.exception.DBException;
import org.rlf.lab.tool.runtime.Action;
import org.rlf.log.RLF_Log;

/**
 * Herramienta contenida en el laboratorio. Contiene los datos más importantes y
 * necesarios para la ejecución. Los datos son obtenidos de las propias bases de
 * datos.
 * 
 * @author Carlos A. Rodriguez Mecha
 * @version 0.1
 */
public class Tool {

	// Enumeración:
	/** Estados posibles de una herramienta. */
	public enum ToolStatus {
		/** No se ha conectado al laboratorio aún o necesita ser reseteada. */
		OFF,
		/** Activa y preparada. */
		ACTIVE,
		/**
		 * Uno de sus acciones está siendo ejecutada. Sólo se permite ejecutar
		 * una acción a la vez.
		 */
		RUNNING;
	};

	// Constantes:
	/** Nombre de la acción de reset. */
	public final static String RESETTER = "resetter";

	// Atributos:
	/** Identificador de la herramienta. */
	private int id;
	/** Ruta donde se encuentran los ficheros. */
	private String path;
	/** Clave asignada a la herramienta por el proveedor. */
	private String key;
	/** Indica si es de datos. */
	private boolean dataTool;
	/** Estado actual de la herramienta. */
	private ToolStatus status;
	/** Indica si tiene activa la salida estándar y error. */
	private boolean outStream;
	/** Indica si tiene activa su stdin. */
	private boolean inStream;

	// Constructores:
	/**
	 * Constructor por defecto.
	 * 
	 * @param id
	 *            Identificador de la herramienta
	 * @param path
	 *            Ruta de los ficheros de la herramienta.
	 * @param key
	 *            Clave de proporcionada por el proveedor.
	 */
	protected Tool(int id, String path, String key) {
		this.id = id;
		this.path = path;
		this.key = key;
		this.status = ToolStatus.OFF;
	}

	// Métodos getters y setters:
	/**
	 * Obtiene el identificador único de la herramienta. Este identificador es
	 * enviador por el proveedor.
	 * 
	 * @return El identificador.
	 */
	public int getId() {
		return id;
	}

	/**
	 * Ruta donde se encuentran los ficheros. No debe tener la barra de
	 * directorio al final.
	 * 
	 * @return La ruta de los ficheros.
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Obtiene la clave proporcionada por el proveedor.
	 * 
	 * @return La clave.
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Indica si la herramienta es de datos, es decir, permite varias conexiones
	 * a su salida y salida de error para obtener datos, pero no permite entrada
	 * de los mismos. Esta herramienta no notificará a los clientes cuando
	 * termina. La acción principal dejará de ejecutarse cuando no haya ningún
	 * cliente más utilizándola.
	 * 
	 * @return Verdadero si es una herramienta de datos.
	 */
	public boolean isDataTool() {
		return dataTool;
	}

	/**
	 * Obtiene el estado actual de la herramienta.
	 * 
	 * @return Estado de la herramienta.
	 */
	public synchronized ToolStatus getStatus() {
		return status;
	}

	/**
	 * La salida estandar y de error de una herramienta está asociada a "stdout"
	 * y "stderr" en sistemas Linux. Representa la impresión por pantalla.
	 * 
	 * @return Verdadero si la herramienta cuenta con salida de datos.
	 */
	public boolean hasOutStream() {
		return outStream;
	}

	/**
	 * La entrada estándar es el medio básico para introducir datos en el
	 * programa ("stdin") en los sistemas Linux.
	 * 
	 * @return Verdadero si la herramienta acepta la utilización de la entrada
	 *         estándar.
	 */
	public boolean hasInStream() {
		return inStream;
	}

	/**
	 * Modifica el estado actual de la herramienta.
	 * 
	 * @param status
	 *            Nuevo estado.
	 */
	public synchronized void setStatus(ToolStatus status) {
		this.status = status;
	}

	/**
	 * Convierte la herramienta a una herramienta de datos. Se utiliza en el
	 * proceso de creación.
	 * 
	 * @param dataTool
	 *            Verdadero si la herramienta pasará a a ser una herramienta de
	 *            datos.
	 */
	public void setDataTool(boolean dataTool) {
		this.dataTool = dataTool;
	}

	/**
	 * Activa o desactiva la salida estándar y de error. Se utiliza en el
	 * proceso de creación.
	 * 
	 * @param outStream
	 *            Indica si se activa el la salida de la acción.
	 */
	public void hasOutStream(boolean outStream) {
		this.outStream = outStream;
	}

	/**
	 * Activa o desactiva la entrada de datos. Se utiliza en el proceso de
	 * creación.
	 * 
	 * @param inStream
	 *            Indica si se activa el stdin.
	 */
	public void hasInStream(boolean inStream) {
		this.inStream = inStream;
	}

	// Métodos de ejecución:
	/**
	 * Obtiene los datos de ejecución de la herramienta. Puede variar
	 * dependiendo del estado de finalización de la acción. Los parámetros
	 * tienen como clave su propio nombre, el estado final como "status" y las
	 * excepciones como "exception". El valor de estas será una cadena con su
	 * nombre entre corchetes y a continuación su descripción.
	 * 
	 * @param action
	 *            Acción ejecutada.
	 * @return Lista de cambios. Puede estar vacía.
	 * @throws DBException
	 *             Problema con la base de datos de la herramienta.
	 */
	public HashMap<String, String> showChanges(String action)
			throws DBException {

		HashMap<String, String> changes = new HashMap<String, String>();
		PreparedStatement stmt = null;
		ResultSet rs = null;
		String selStatus = "SELECT value, description FROM status WHERE action = ?";
		String selExcept = "SELECT name, description FROM exec_exception WHERE action = ?";
		String selParam = "SELECT p.name, p.value FROM parameter p, action_parameter ap WHERE p.name = ap.parameter AND (ap.parameter_type IN ('out', 'inout')) AND ap.action = ?";
		DBHelper db = new DBHelper();

		// 1. Conexíon con la base de datos.
		Connection conn = db.connectToTool(this.path);
		if (conn == null) {
			RLF_Log.LabLog().severe(
					"[CHANGES] No se ha podido conectar con la base de datos de la herramienta "
							+ this.path);
			throw new DBException();
		}

		// 2. Obtención del estado de la ejecución.
		try {

			stmt = conn.prepareStatement(selStatus);
			stmt.setString(1, action);
			rs = stmt.executeQuery();

			if (rs.next()) {
				changes.put("status",
						"(" + rs.getInt(1) + ") " + rs.getString(2));
			}
			rs.close();
			stmt.close();

			// 3. Obtención de las excepciones.
			stmt = conn.prepareStatement(selExcept);
			stmt.setString(1, action);
			rs = stmt.executeQuery();
			int nexception = 0;

			while (rs.next()) {
				changes.put("exception(" + ++nexception +  ")",
						"[" + rs.getString(1) + "] " + rs.getString(2));
			}
			rs.close();
			stmt.close();

			// 4. Obtención de los parámetros.
			if (this.getStatus() == ToolStatus.ACTIVE) {
				stmt = conn.prepareStatement(selParam);
				stmt.setString(1, action);
				rs = stmt.executeQuery();

				while (rs.next()) {
					if (rs.getString(2) != null)
						changes.put(rs.getString(1), rs.getString(2));
				}
				rs.close();
				stmt.close();
			}
		} catch (SQLException e) {
			RLF_Log.LabLog().severe(
					"[CHANGES] Problema con la base de datos de la herramienta "
							+ this.id);
			try {
				rs.close();
				stmt.close();
			} catch (Exception ex) {
			}
			db.close(conn);
			throw new DBException();

		}

		db.close(conn);

		return changes;
	}

	/**
	 * Ejecuta la acción indicada. Esta acción puede ser supervisada por el
	 * usuario si es válido y se conecta a los sockets correspondientes.
	 * 
	 * @param action
	 *            Nombre de la acción.
	 * @param socketIn
	 *            Socket de entrada ya configurado. Null si no se necesita.
	 * @param socketOut
	 *            Socket de salida ya configurado. Null si no se necesita.
	 * @param changes
	 *            Lista de forma <parameter, value> para modificar los valores
	 *            de los parámetros del comando.
	 * @return Instancia de ejecución.
	 * @throws RuntimeException
	 *             Problema con la ejecución. La acción introducida no
	 *             corresponde a ninguna válida o los sockets no han podido ser
	 *             abiertos.
	 * @throws DBException
	 *             Problema con la base de datos de la herramienta. No se
	 *             ejecutará la acción.
	 */
	public Action execute(String action, ServerSocketChannel socketIn,
			ServerSocketChannel socketOut, HashMap<String, String> changes)
			throws RuntimeException, DBException {

		int timeout = 0;
		String value;

		DBHelper db = new DBHelper();
		String sel = "SELECT value, timeout FROM action WHERE name = ?";
		String updParam = "UPDATE parameter SET modified = 1, value = ? WHERE name = ?";
		String[] cleans = {
				"UPDATE attribute SET value = ? WHERE name = 'EXEC_ACTION'",
				"DELETE FROM status WHERE action = ?",
				"DELETE FROM exec_exception WHERE action = ?",
				"UPDATE attribute SET value = 'false' WHERE name = 'CONNECT'",
				"UPDATE parameter SET value = NULL, modified = 0" };
		PreparedStatement stmt = null;
		ResultSet rs = null;
		Connection conn = db.connectToTool(this.path);
		if (conn == null) {
			if (socketIn != null) {
				try {
					socketIn.close();
				} catch (IOException ioe){
					
				}
			}
			if (socketOut != null) {
				try {
					socketOut.close();
				} catch (IOException ioe){
					
				}
			}
			throw new DBException();
		}
			
		// 1. Se obtiene el comando.
		try {
			stmt = conn.prepareStatement(sel);
			stmt.setString(1, action);
			rs = stmt.executeQuery();
			if (!rs.next()) {
				rs.close();
				stmt.close();
				db.close(conn);
				if (socketIn != null) {
					try {
						socketIn.close();
					} catch (IOException ioe){
						
					}
				}
				if (socketOut != null) {
					try {
						socketOut.close();
					} catch (IOException ioe){
						
					}
				}
				throw new RuntimeException();
			}
			value = rs.getString("value");
			timeout = rs.getInt("timeout");
			rs.close();
			stmt.close();

			// 2. Se limpia la ejecución.
			for (int i = 0; i < cleans.length; i++) {
				stmt = conn.prepareStatement(cleans[i]);
				if (i <= 2)
					stmt.setString(1, action);
				stmt.execute();
				stmt.close();
			}

			// 3. Se modifican los parámetros.
			if (changes != null) {
				stmt = conn.prepareStatement(updParam);
				for (Entry<String, String> e : changes.entrySet()) {
					stmt.setString(1, e.getValue());
					stmt.setString(2, e.getKey());
					stmt.addBatch();
				}
				stmt.executeBatch();
			}
			stmt.close();

		} catch (SQLException e) {
			RLF_Log.LabLog()
					.severe("[EXECUTE] La base de datos de la herramienta contiene errores.");
			try {
				rs.close();
				stmt.close();
			} catch (Exception ex) {
			}
			db.close(conn);
			if (socketIn != null) {
				try {
					socketIn.close();
				} catch (IOException ioe){
					
				}
			}
			if (socketOut != null) {
				try {
					socketOut.close();
				} catch (IOException ioe){
					
				}
			}
			throw new DBException();
		}

		db.close(conn);

		Action runtime = new Action(this, value, socketIn, socketOut, timeout);

		// 2. Ejecución.
		runtime.start();

		return runtime;

	}

	/**
	 * Ejecuta el reseteador de la herramienta. Corresponde con el identificador
	 * 0.
	 * 
	 * @return Instancia de la ejecución.
	 * @throws DBException
	 *             Problema con la base de datos de la herramienta. No se
	 *             ejecutará la acción.
	 */
	public Action clean() throws DBException {

		String value;

		DBHelper db = new DBHelper();
		String sel = "SELECT value FROM action WHERE name = ?";

		String[] cleans = {
				"UPDATE attribute SET value = ? WHERE name = 'EXEC_ACTION'",
				"DELETE FROM exec_exception WHERE action = ?",
				"DELETE FROM status WHERE action = ?",
				"UPDATE attribute SET value = 'false' WHERE name = 'CONNECT'",
				"UPDATE parameter SET value = NULL, modified = 0" };
		PreparedStatement stmt = null;
		ResultSet rs = null;
		Connection conn = db.connectToTool(this.path);
		if (conn == null)
			throw new DBException();

		// 1. Se obtiene el comando.
		try {
			stmt = conn.prepareStatement(sel);
			stmt.setString(1, RESETTER);
			rs = stmt.executeQuery();
			if (!rs.next()) {
				rs.close();
				stmt.close();
				db.close(conn);
				throw new DBException();
			}
			value = rs.getString("value");
			rs.close();
			stmt.close();

			// 2. Se limpia la ejecución.
			for (int i = 0; i < cleans.length; i++) {
				stmt = conn.prepareStatement(cleans[i]);
				if (i <= 2)
					stmt.setString(1, RESETTER);
				stmt.execute();
				stmt.close();
			}

		} catch (SQLException e) {
			RLF_Log.LabLog()
					.severe("[CLEAN] La base de datos de la herramienta contiene errores.");
			try {
				rs.close();
				stmt.close();
			} catch (Exception ex) {
			}
			db.close(conn);
			throw new DBException();
		}

		db.close(conn);

		Action runtime = new Action(this, value);

		// 2. Ejecución.
		runtime.start();

		return runtime;

	}
}
