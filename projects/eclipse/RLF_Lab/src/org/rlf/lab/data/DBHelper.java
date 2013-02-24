/**
 * Gestor de la base de datos del laboratorio y herramientas.
 */
package org.rlf.lab.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.*;

import org.rlf.log.RLF_Log;

/**
 * Asistente para las bases de datos de las herramientas y del propio
 * laboratorio.
 * 
 * @author Carlos A. Rodriguez Mecha
 * @version 0.1
 */
public class DBHelper {

	// Constantes:
	/** Nombre de la base de datos del laboratorio. */
	public final static String LABDBNAME = "lab.data";
	/** Nombre de las bases de datos de las herramientas. */
	public final static String TOOLDBNAME = "tool.rlf";

	// Constructor:
	/**
	 * Constructor del asistente.
	 */
	public DBHelper() {

	}

	// Métodos varios:
	/**
	 * Abre una conexión con una base de datos del propio laboratorio.
	 * 
	 * @return Conexión con la base. Null si no ha podido conectarse.
	 */
	public Connection connect() {

		Connection conn = null;

		try {

			Class.forName("org.sqlite.JDBC");
			conn = DriverManager.getConnection("jdbc:sqlite:"
					+ System.getProperty("user.dir", "") + File.separator
					+ "res" + File.separator + LABDBNAME);

		} catch (Exception e) {
			RLF_Log.LabLog().severe(
					"[EXCEPTION] Conexión fallida con la base de datos: "
							+ e.getMessage());
		}

		return conn;

	}

	/**
	 * Abre una conexión con una herramienta concreta.
	 * 
	 * @param path
	 *            Ruta de la base de datos de la herramienta.
	 * @return Conexión con la base. Null si no ha podido conectarse porque no
	 *         se encuentra el fichero.
	 */
	public Connection connectToTool(String path) {

		Connection conn = null;

		try {

			Class.forName("org.sqlite.JDBC");
			conn = DriverManager.getConnection("jdbc:sqlite:" + path
					+ File.separator + TOOLDBNAME);

		} catch (Exception e) {
			RLF_Log.LabLog().severe(
					"[EXCEPTION] Conexión fallida con la herramienta: "
							+ e.getMessage());
		}

		return conn;

	}

	/**
	 * Abre una conexión con una base de datos del proveedor.
	 * 
	 * @param host
	 *            Máquina del proveedor.
	 * @param port
	 *            Puerto de conexión con el proveedor.
	 * @param user
	 *            Usuario de conexión asignado al laboratorio.
	 * @param pass
	 *            Contraseña de ese usuario.
	 * @return Conexión con la base. Null si no ha podido conectarse.
	 */
	public Connection connectToProvider(String host, int port, String user,
			String pass) {

		Connection conn = null;

		try {

			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:mysql://" + host + ":"
					+ port + "/rlf", user, pass);

		} catch (Exception e) {
			RLF_Log.LabLog().severe(
					"[EXCEPTION] Conexión fallida con el proveedor: "
							+ e.getMessage());
		}

		return conn;

	}

	/**
	 * Cierra la conexión introducida.
	 * 
	 * @param conn
	 *            Conexión a cerrar.
	 */
	public void close(Connection conn) {
		try {
			conn.close();
		} catch (SQLException e) {
			RLF_Log.LabLog().severe(
					"[EXCEPTION] No se ha podido cerrar la conexión con la base de datos: "
							+ e.getMessage());
		}
	}

	// Métodos de la base de datos:
	/**
	 * Crea una base de datos de una herramienta o la propia del laboratorio. Le
	 * introduce la estructura establecida.
	 * 
	 * @param path
	 *            Ruta de la herramienta. Si es null crea la base de datos del
	 *            laboratorio.
	 * @return Verdadero si ha podido crearla.
	 */
	public boolean createDB(String path) {

		Connection conn = null;

		if (path == null)
			conn = connect();
		else
			conn = connectToTool(path);

		if (conn == null)
			return false;

		String sqlfile;
		StringBuilder sql = new StringBuilder();
		if (path == null)
			sqlfile = System.getProperty("user.dir", "") + File.separator
					+ "res" + File.separator + "labdata.sql";
		else
			sqlfile = System.getProperty("user.dir", "") + File.separator
					+ "res" + File.separator + "tool.sql";

		try {

			// 1. Lectura del fichero de estructura.
			File file = new File(sqlfile);
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			char[] buffer = new char[1024];
			while (br.read(buffer) >= 0) {
				sql.append(buffer);
				for (int i = 0; i < buffer.length; i++)
					buffer[i] = 0;
			}

			br.close();
			fr.close();

			String[] sqlstatement = sql.toString().split(";");
			conn.setAutoCommit(false);

			// 2. Insercción en la base.
			PreparedStatement stmt;
			for (String query : sqlstatement) {
				if (query.trim().length() == 0)
					continue;
				stmt = conn.prepareStatement(query);
				stmt.execute();
				stmt.close();
			}
			conn.commit();

		} catch (Exception e) {
			RLF_Log.LabLog().severe(
					"[CREATEDB] Error creando la base de datos "
							+ e.getMessage());
			close(conn);
			return false;
		}

		close(conn);
		return true;

	}

	/**
	 * Elimina la base de datos de la herramienta o del laboratorio.
	 * 
	 * @param path
	 *            Ruta de la herramienta. Si es null borra la base de datos del
	 *            laboratorio.
	 * @return Verdadero si ha podido eliminar la base de datos.
	 */
	public boolean deleteDB(String path) {

		String name;
		if (path != null)
			name = path + File.separator + TOOLDBNAME;
		else
			name = System.getProperty("user.dir", "") + File.separator + "res"
					+ File.separator + LABDBNAME;

		File db = new File(name);
		return db.delete();

	}

}
