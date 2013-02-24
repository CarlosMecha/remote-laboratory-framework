/**
 * Gestor de herramientas.
 */
package org.rlf.lab.tool;

import java.io.File;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.jdom.*;
import org.rlf.lab.LabContext;
import org.rlf.lab.data.DBHelper;
import org.rlf.log.RLF_Log;

import com.google.gson.*;

/**
 * Asistente de creación y mantenimiento de las herramientas actuales del
 * laboratorio.
 * 
 * @author Carlos A. Rodriguez Mecha
 * @version 0.1
 */
public class ToolManager {

	// Atributos:
	/** Instancia del asistente. */
	private static ToolManager instance;
	/** Lista de las herramientas activas actuales. */
	private HashMap<Integer, Tool> tools;
	/** Contexto del Box. */
	private LabContext context;

	// Constructor:
	/**
	 * Constructor del asistente.
	 * 
	 * @param context
	 *            Contexto.
	 */
	private ToolManager(LabContext context) {
		this.tools = new HashMap<Integer, Tool>();
		this.context = context;
	}

	/**
	 * Obtiene la instancia del asistente de servicios. No es necesario el
	 * parámetro una vez ya ha sido invocado.
	 * 
	 * @param context
	 *            Contexto.
	 * @return Instancia.
	 */
	public static ToolManager Instance(LabContext context) {
		if (instance == null)
			instance = new ToolManager(context);
		return instance;
	}

	/**
	 * Obtiene la lista completa de las herramientas actuales del laboratorio.
	 * 
	 * @return Lista de las herramientas con su identificador.
	 */
	public HashMap<Integer, Tool> getTools() {
		return tools;
	}

	// Métodos varios:
	/**
	 * Lee las informaciones de las herramientas para su posterior activación.
	 * Estas se activarán una vez se haya completado su reseteo. No se leerá si
	 * ya está activo.
	 * 
	 * @return Verdadero si ha podido leer todas las herramientas.
	 */
	public boolean readTools() {

		if (this.context.isArmed())
			return false;

		DBHelper db = new DBHelper();
		int id;
		String path, key;
		Tool tool;
		PreparedStatement stmt, toolStmt;
		ResultSet rs, toolRs;
		String selTools = "SELECT * FROM tool";
		String selAttrs = "SELECT value FROM attribute WHERE name = ?";
		Connection toolDB, conn = db.connect();

		if (conn == null) {
			RLF_Log.LabLog()
					.severe("[READ] No se puede establecer conexión con la base de datos del laboratorio.");
			return false;
		}

		try {

			stmt = conn.prepareStatement(selTools);
			rs = stmt.executeQuery();

			while (rs.next()) {

				// 1. Datos básicos.
				id = rs.getInt("id");
				key = rs.getString("key");
				path = rs.getString("path");

				tool = new Tool(id, path, key);

				// 2. Datos de la herramienta.
				toolDB = db.connectToTool(path);
				if (toolDB == null) {
					RLF_Log.LabLog()
							.severe("[READ] No se localiza la base de datos de la herramienta.");
					db.close(conn);
					return false;
				}

				toolStmt = toolDB.prepareStatement(selAttrs);

				// 2.1 Data.
				toolStmt.setString(1, "DATA");
				toolRs = toolStmt.executeQuery();
				if (!toolRs.next()) {
					RLF_Log.LabLog()
							.severe("[READ] La base de datos de la herramienta tiene un formato incorrecto.");
					toolRs.close();
					toolStmt.close();
					db.close(toolDB);
					db.close(conn);
					return false;
				}
				tool.setDataTool(toolRs.getString("value").compareToIgnoreCase(
						"true") == 0);
				toolRs.close();

				// 2.2 Instream.
				toolStmt.setString(1, "INSTREAM");
				toolRs = toolStmt.executeQuery();
				if (!toolRs.next()) {
					RLF_Log.LabLog()
							.severe("[READ] La base de datos de la herramienta tiene un formato incorrecto.");
					toolRs.close();
					toolStmt.close();
					db.close(toolDB);
					db.close(conn);
					return false;
				}
				tool.hasInStream(toolRs.getString("value").compareToIgnoreCase(
						"true") == 0);
				toolRs.close();

				// 2.3 Outstream.
				toolStmt.setString(1, "OUTSTREAM");
				toolRs = toolStmt.executeQuery();
				if (!toolRs.next()) {
					RLF_Log.LabLog()
							.severe("[READ] La base de datos de la herramienta tiene un formato incorrecto.");
					toolRs.close();
					toolStmt.close();
					db.close(toolDB);
					db.close(conn);
					return false;
				}
				tool.hasOutStream(toolRs.getString("value")
						.compareToIgnoreCase("true") == 0);

				toolRs.close();
				toolStmt.close();
				db.close(toolDB);

				this.tools.put(id, tool);
			}

			rs.close();
			stmt.close();

		} catch (SQLException e) {
			RLF_Log.LabLog()
					.severe("[READ] La base de datos del laboratorio contiene errores.");
			db.close(conn);
			return false;
		}

		return true;

	}

	/**
	 * Obtiene el estado de todas las herramientas localizadas en el
	 * laboratorio.
	 * 
	 * @return Lista de identificadores con el estado asociado.
	 */
	public HashMap<Integer, String> status() {

		HashMap<Integer, String> list = new HashMap<Integer, String>();

		for (Entry<Integer, Tool> entry : this.tools.entrySet()) {
			list.put(entry.getKey(), entry.getValue().getStatus().toString());
		}

		return list;
	}

	/**
	 * A partir del XML, crea los datos de una herramienta nueva en la base de
	 * datos SQLite propia, y además, obtiene los datos en formato JSON para su
	 * envío al proveedor. Después lo registra en la base de datos del
	 * laboratorio. No se realizará si está activo.
	 * 
	 * @param id
	 *            Identificador de la herramienta.
	 * @param key
	 *            Clave proporcionada por el proveedor.
	 * @param xml
	 *            Documento JDOM XML de la herramienta. Ya debe estar validado.
	 * @param providerDB
	 *            Conexión con el proveedor. Debe estar abierta. No se cerrará.
	 * @return Verdadero si ha podido realizar toda la operación.
	 * @throws SQLException
	 *             Fallo en la recuperación de un error. Debe revisarse la base
	 *             de datos del proveedor.
	 */
	public boolean registry(int id, String key, Document xml,
			Connection providerDB) throws SQLException {

		if (this.context.isArmed())
			return false;

		String regrex = "'|\"";
		Element root = xml.getRootElement(), node;
		DBHelper db = new DBHelper();
		JsonObject jtool = new JsonObject(), jconstant, jparameter, jaction, jsocket, jactparam;
		JsonArray jconstants = new JsonArray(), jparameters = new JsonArray(), jactions = new JsonArray(), jsockets = new JsonArray(), jactparams = new JsonArray();
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");
		LinkedList<String> params = new LinkedList<String>();

		// 0. Conexiónes y creación de la base de datos.
		Connection toolDB, labDB;
		try {
			providerDB.setAutoCommit(false);
		} catch (SQLException e) {
			RLF_Log.LabLog().severe(
					"[REGISTRY] Problema con la conexión con el proveedor.");
			return false;
		}
		if ((labDB = db.connect()) == null) {
			RLF_Log.LabLog()
					.severe("[REGISTRY] No se puede establecer conexión con la base de datos del laboratorio.");
			return false;
		}

		Savepoint providerSP;
		PreparedStatement stmtTool = null, stmtProvider = null, stmtLab = null;
		@SuppressWarnings("unused")
		Statement saveStmt = null;
		try {
			labDB.setAutoCommit(false);
			saveStmt = providerDB.createStatement();
			providerSP = providerDB.setSavepoint();
		} catch (SQLException e) {
			RLF_Log.LabLog()
					.severe("[REGISTRY] No se han podido crear los puntos de restauración.");
			db.close(labDB);
			return false;
		}

		String insAttribute = "UPDATE attribute SET value = ? WHERE name = ?";
		String insToolProvider = "INSERT INTO tool (id, toolkey, name, description, version, admin, role, data, instream, outstream, lab, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		String insToolLab = "INSERT INTO tool (id, path, key) VALUES (?, ?, ?)";
		String insConstant = "INSERT INTO constant (name, description, dtype, value) VALUES (?, ?, ?, ?)";
		String insConstantProvider = "INSERT INTO constant (tool, name, description, dtype, value) VALUES (?, ?, ?, ?, ?)";
		String insParameter = "INSERT INTO parameter (name, description, dfl, max, min, dtype, value, modified) VALUES (?, ?, ?, ?, ?, ?, NULL, 0)";
		String insParameterProvider = "INSERT INTO parameter (tool, name, description, dfl, max, min, dtype) VALUES (?, ?, ?, ?, ?, ?, ?)";
		String insAction = "INSERT INTO action (name, description, value, timeout) VALUES (?, ?, ?, ?)";
		String insActionProvider = "INSERT INTO action (tool, name, description, timeout) VALUES (?, ?, ?, ?)";
		String insSocket = "INSERT INTO socket (action, port, protocol, type, mode) VALUES (?, ?, ?, ?, ?)";
		String insSocketProvider = "INSERT INTO socket (tool, action, port, protocol, type, mode) VALUES (?, ?, ?, ?, ?, ?)";
		String insActParameter = "INSERT INTO action_parameter (action, parameter, parameter_type) VALUES (?, ?, ?)";
		String insActParameterProvider = "INSERT INTO action_parameter (tool, action, parameter, parameter_type) VALUES (?, ?, ?, ?)";
		String updTool = "UPDATE tool SET json = ? WHERE id = ?";
		String selPath = "SELECT path FROM tool WHERE path = ?";

		// 1. Atributos.
		String path, name, description, version, admin;
		boolean data, inStream, outStream;
		int role;

		// 1.1 Path.
		path = root.getAttributeValue("path").replaceAll(regrex, "?");
		if (path.compareTo(root.getAttributeValue("path")) != 0) {
			RLF_Log.LabLog().warning(
					"[REGISTRY] El path contiene caracteres inválidos.");
			db.close(labDB);
			return false;
		}

		// 1.1.1 Comprobación del path.
		try {
			stmtLab = labDB.prepareStatement(selPath);
			stmtLab.setString(1, path);
			ResultSet rs = stmtLab.executeQuery();
			if (rs.next()) {
				RLF_Log.LabLog().warning(
						"Ya existe una herramienta registrada con ese path.");
				throw new SQLException();
			}
			rs.close();
			stmtLab.close();
			stmtLab = null;
		} catch (SQLException e) {
			db.close(labDB);
			return false;
		}
		File dir = new File(path);
		if (!dir.exists() || !dir.isDirectory()) {
			RLF_Log.LabLog().warning(
					"El path introducido no es un directorio o no existe.");
			db.close(labDB);
			return false;
		}

		// 1.1.2 Creación de la base de datos de la herramienta.
		File tooldb = new File(path + File.separator + DBHelper.TOOLDBNAME);
		if (tooldb.exists()) {
			tooldb.delete();
		}
		if (!db.createDB(path)) {
			RLF_Log.LabLog()
					.warning(
							"[REGISTRY] No se ha podido crear la base de datos de la herramienta.");
			db.close(labDB);
			return false;
		}
		if ((toolDB = db.connectToTool(path)) == null) {
			RLF_Log.LabLog()
					.warning(
							"[REGISTRY] No se puede establecer conexión con la base de datos de la herramienta.");
			db.close(labDB);
			return false;
		}
		try {
			toolDB.setAutoCommit(false);
		} catch (SQLException e) {
			RLF_Log.LabLog().severe(
					"[REGISTRY] Problema con la conexión con la herramienta.");
			db.close(labDB);
			db.close(toolDB);
			return false;
		}

		// 1.2 Data.
		if (root.getAttributeValue("data").compareToIgnoreCase("true") == 0) {
			data = true;
		} else if (root.getAttributeValue("data").compareToIgnoreCase("false") == 0) {
			data = false;
		} else {
			RLF_Log.LabLog()
					.warning("[REGISTRY] Valor de \"data\" incorrecto.");
			db.close(toolDB);
			db.close(labDB);
			return false;
		}

		// 1.3 InStream.
		inStream = (root.getChild("in-stream") != null);

		// 1.4 OutStream.
		outStream = (root.getChild("out-stream") != null);

		// 1.5 Name.
		node = root.getChild("attributes");
		name = node.getChild("name").getText().replaceAll(regrex, "?");
		if (name.length() > 50) {
			RLF_Log.LabLog()
					.warning("[REGISTRY] El nombre es demasiado largo.");
			db.close(toolDB);
			db.close(labDB);
			return false;
		}

		// 1.6 Description.
		description = node.getChild("description").getText()
				.replaceAll(regrex, "?");
		if (description.length() > 2300) {
			RLF_Log.LabLog().warning(
					"[REGISTRY] La descripción es demasiado larga.");
			db.close(toolDB);
			db.close(labDB);
			return false;
		}

		// 1.7 Version.
		version = node.getChild("version").getText().replaceAll(regrex, "?");
		if (version.length() > 20) {
			RLF_Log.LabLog().warning(
					"[REGISTRY] La versión es demasiado larga.");
			db.close(toolDB);
			db.close(labDB);
			return false;
		}

		// 1.8 Admin.
		admin = node.getChild("admin").getText().replaceAll(regrex, "?");
		if (admin.length() > 40) {
			RLF_Log.LabLog().warning("[REGISTRY] Ese administrador no existe.");
			db.close(toolDB);
			db.close(labDB);
			return false;
		}

		// 1.9 Role.
		try {
			role = Integer.parseInt(node.getChild("role").getText());
		} catch (Exception e) {
			RLF_Log.LabLog().warning(
					"[REGISTRY] \"role\" no es un número positivo.");
			db.close(toolDB);
			db.close(labDB);
			return false;
		}

		// 2. Insercción de atributos.

		// 2.1 Insercción en la herramienta.
		try {

			stmtTool = toolDB.prepareStatement(insAttribute);

			stmtTool.setString(1, new Integer(id).toString());
			stmtTool.setString(2, "ID");
			stmtTool.addBatch();

			stmtTool.setString(1, key);
			stmtTool.setString(2, "KEY");
			stmtTool.addBatch();

			stmtTool.setString(1, path);
			stmtTool.setString(2, "PATH");
			stmtTool.addBatch();

			stmtTool.setString(1, name);
			stmtTool.setString(2, "NAME");
			stmtTool.addBatch();

			stmtTool.setString(1, description);
			stmtTool.setString(2, "DESCRIPTION");
			stmtTool.addBatch();

			stmtTool.setString(1, version);
			stmtTool.setString(2, "VERSION");
			stmtTool.addBatch();

			stmtTool.setString(1, admin);
			stmtTool.setString(2, "ADMIN");
			stmtTool.addBatch();

			stmtTool.setString(1, new Integer(role).toString());
			stmtTool.setString(2, "ROLE");
			stmtTool.addBatch();

			stmtTool.setString(1, dateFormat.format(new Timestamp(
					new java.util.Date().getTime())));
			stmtTool.setString(2, "INSDATE");
			stmtTool.addBatch();

			stmtTool.setString(1, new Boolean(data).toString());
			stmtTool.setString(2, "DATA");
			stmtTool.addBatch();

			stmtTool.setString(1, new Boolean(inStream).toString());
			stmtTool.setString(2, "INSTREAM");
			stmtTool.addBatch();

			stmtTool.setString(1, new Boolean(outStream).toString());
			stmtTool.setString(2, "OUTSTREAM");
			stmtTool.addBatch();

			stmtTool.executeBatch();
			stmtTool.close();

		} catch (SQLException e) {
			RLF_Log.LabLog()
					.severe("[REGISTRY] No se pueden introducir los atributos en la herramienta.");
			db.close(toolDB);
			db.close(labDB);
			return false;
		} finally {
			if (stmtTool != null) {
				try {
					stmtTool.close();
				} catch (SQLException e) {
				}
			}
		}

		// 2.2 Insercción en el proveedor.
		try {

			stmtProvider = providerDB.prepareStatement(insToolProvider);

			stmtProvider.setInt(1, id);
			stmtProvider.setString(2, key);
			stmtProvider.setString(3, name);
			stmtProvider.setString(4, description);
			stmtProvider.setString(5, version);
			stmtProvider.setString(6, admin);
			stmtProvider.setInt(7, role);
			stmtProvider.setInt(8, data ? 1 : 0);
			stmtProvider.setInt(9, inStream ? 1 : 0);
			stmtProvider.setInt(10, outStream ? 1 : 0);
			stmtProvider.setString(11, this.context.getName());
			stmtProvider.setString(12, "OFFLINE");

			stmtProvider.execute();
			stmtProvider.close();

		} catch (SQLException e) {
			RLF_Log.LabLog()
					.severe("[REGISTRY] No se puede introducir la herramienta en el proveedor.");
			toolDB.rollback();
			db.close(toolDB);
			db.close(labDB);
			return false;
		} finally {
			if (stmtProvider != null) {
				try {
					stmtProvider.close();
				} catch (SQLException e) {
				}
			}
		}

		// 2.3 Creación del json.
		jtool.addProperty("id", new Integer(id).toString());
		jtool.addProperty("name", name);
		jtool.addProperty("description", description);
		jtool.addProperty("version", version);
		jtool.addProperty("data", new Boolean(data).toString());
		jtool.addProperty("in_stream", new Boolean(inStream).toString());
		jtool.addProperty("out_stream", new Boolean(outStream).toString());

		// 3. Constantes:
		node = root.getChild("constants");
		stmtTool = null;
		stmtProvider = null;
		try {

			stmtTool = toolDB.prepareStatement(insConstant);
			stmtProvider = providerDB.prepareStatement(insConstantProvider);

		} catch (SQLException e) {
			RLF_Log.LabLog().severe(
					"[REGISTRY] No se pueden preparar las sentencias.");
			try {
				if (stmtTool != null)
					stmtTool.close();
			} catch (SQLException sqle) {
			}
			toolDB.rollback();
			providerDB.rollback(providerSP);
			db.close(toolDB);
			db.close(labDB);
			return false;
		}

		for (Object element : node.getChildren()) {

			String cname, cdescription, cdtype, cvalue;
			Element child = (Element) element;
			jconstant = new JsonObject();

			// 3.1 Nombre
			cname = child.getAttributeValue("name").replaceAll(regrex, "?");
			if (cname.length() > 128) {
				RLF_Log.LabLog().warning(
						"[REGISTRY] El nombre de la constante es muy largo.");
				try {
					stmtTool.close();
					stmtProvider.close();
				} catch (SQLException e) {
				}
				toolDB.rollback();
				providerDB.rollback(providerSP);
				db.close(toolDB);
				db.close(labDB);

				return false;
			}

			// 3.2 Descripción.
			cdescription = child.getChild("description").getText()
					.replaceAll(regrex, "?");
			if (cdescription.length() > 500) {
				RLF_Log.LabLog()
						.warning(
								"[REGISTRY] La descripción de la constante es muy larga.");
				try {
					stmtTool.close();
					stmtProvider.close();
				} catch (SQLException e) {
				}
				toolDB.rollback();
				providerDB.rollback(providerSP);
				db.close(toolDB);
				db.close(labDB);

				return false;
			}

			// 3.3 Dtype.
			cdtype = child.getAttributeValue("data-type");

			// 3.4 Valor.
			cvalue = child.getChild("value").getText().replaceAll(regrex, "?");

			// 4. Insercción de la constante.

			// 4.1 Insercción en la herramienta.
			try {

				stmtTool.setString(1, cname);
				stmtTool.setString(2, cdescription);
				stmtTool.setString(3, cdtype);
				stmtTool.setString(4, cvalue);

				stmtTool.addBatch();

			} catch (SQLException e) {
				RLF_Log.LabLog()
						.severe("[REGISTRY] Insercción de comando fallida en la herramienta.");
				try {
					stmtTool.close();
					stmtProvider.close();
				} catch (SQLException sqle) {
				}
				toolDB.rollback();
				providerDB.rollback(providerSP);
				db.close(toolDB);
				db.close(labDB);

				return false;
			}

			// 4.2 Insercción en el proveedor.
			try {

				stmtProvider.setInt(1, id);
				stmtProvider.setString(2, cname);
				stmtProvider.setString(3, cdescription);
				stmtProvider.setString(4, cdtype);
				stmtProvider.setString(5, cvalue);

				stmtProvider.addBatch();

			} catch (SQLException e) {
				RLF_Log.LabLog()
						.severe("[REGISTRY] Insercción de comando fallida en el proveedor.");
				try {
					stmtTool.close();
					stmtProvider.close();
				} catch (SQLException sqle) {
				}
				toolDB.rollback();
				providerDB.rollback(providerSP);
				db.close(toolDB);
				db.close(labDB);

				return false;
			}

			// 4.3 Creación json.
			jconstant.addProperty("name", cname);
			jconstant.addProperty("description", cdescription);
			jconstant.addProperty("data-type", cdtype);
			jconstant.addProperty("value", cvalue);
			jconstants.add(jconstant);

		}

		// 4.4 Insercciones en la herramienta.
		try {
			stmtTool.executeBatch();
			stmtTool.close();
		} catch (SQLException e) {
			RLF_Log.LabLog()
					.severe("[REGISTRY] Insercción de comandos fallida en la herramienta.");
			try {
				stmtTool.close();
				stmtProvider.close();
			} catch (SQLException sqle) {
			}
			toolDB.rollback();
			providerDB.rollback(providerSP);
			db.close(toolDB);
			db.close(labDB);

			return false;
		}

		// 4.5 Insercciones en el proveedor.
		try {
			stmtProvider.executeBatch();
			stmtProvider.close();
		} catch (SQLException e) {
			RLF_Log.LabLog()
					.severe("[REGISTRY] Insercción de comandos fallida en el proveedor.");
			try {
				stmtProvider.close();
			} catch (SQLException sqle) {
			}
			toolDB.rollback();
			providerDB.rollback(providerSP);
			db.close(toolDB);
			db.close(labDB);

			return false;
		}

		// 4.6 Insercciones en json.
		jtool.add("constants", jconstants);

		if (!data) {

			// 5. Parámetros:
			node = root.getChild("parameters");
			stmtTool = null;
			stmtProvider = null;
			try {

				stmtTool = toolDB.prepareStatement(insParameter);
				stmtProvider = providerDB
						.prepareStatement(insParameterProvider);

			} catch (SQLException e) {
				RLF_Log.LabLog().severe(
						"[REGISTRY] No se pueden preparar las sentencias.");
				try {
					if (stmtTool != null)
						stmtTool.close();
				} catch (SQLException sqle) {
				}
				toolDB.rollback();
				providerDB.rollback(providerSP);
				db.close(toolDB);
				db.close(labDB);
				return false;
			}

			for (Object element : node.getChildren()) {

				String pname, pdescription, pdfl = null, pmax = null, pmin = null, pdtype;
				Element child = (Element) element;
				jparameter = new JsonObject();

				// 5.1 Nombre.
				pname = child.getAttributeValue("name").replaceAll(regrex, "?");
				if (pname.length() > 128) {
					RLF_Log.LabLog().warning(
							"[REGISTRY] El nombre del parámetro es muy largo.");
					try {
						stmtTool.close();
						stmtProvider.close();
					} catch (SQLException e) {
					}
					toolDB.rollback();
					providerDB.rollback(providerSP);
					db.close(toolDB);
					db.close(labDB);

					return false;
				}
				params.add(pname);

				// 5.2 Descripción.
				pdescription = child.getChild("description").getText()
						.replaceAll(regrex, "?");
				if (pdescription.length() > 500) {
					RLF_Log.LabLog()
							.warning(
									"[REGISTRY] La descripción del parámetro es muy larga.");
					try {
						stmtTool.close();
						stmtProvider.close();
					} catch (SQLException e) {
					}
					toolDB.rollback();
					providerDB.rollback(providerSP);
					db.close(toolDB);
					db.close(labDB);

					return false;
				}

				// 5.3 Dlf.
				if (child.getChild("dfl") != null) {
					pdfl = child.getChild("dfl").getText()
							.replaceAll(regrex, "?");
				}

				// 5.4 Max.
				if (child.getChild("max") != null) {
					pmax = child.getChild("max").getText()
							.replaceAll(regrex, "?");
				}

				// 5.5 Min.
				if (child.getChild("min") != null) {
					pmin = child.getChild("min").getText()
							.replaceAll(regrex, "?");
				}

				// 5.6 Dtype.
				pdtype = child.getAttributeValue("data-type");

				// 6. Insercción del parámetro.

				// 6.1 Insercción en la herramienta.
				try {

					stmtTool.setString(1, pname);
					stmtTool.setString(2, pdescription);
					if (pdfl != null)
						stmtTool.setString(3, pdfl);
					else
						stmtTool.setNull(3, java.sql.Types.VARCHAR);
					if (pmax != null)
						stmtTool.setString(4, pmax);
					else
						stmtTool.setNull(4, java.sql.Types.VARCHAR);
					if (pmin != null)
						stmtTool.setString(5, pmin);
					else
						stmtTool.setNull(5, java.sql.Types.VARCHAR);
					stmtTool.setString(6, pdtype);

					stmtTool.addBatch();

				} catch (SQLException e) {
					RLF_Log.LabLog()
							.severe("[REGISTRY] Insercción del parámetro fallida en la herramienta.");
					try {
						stmtTool.close();
						stmtProvider.close();
					} catch (SQLException sqle) {
					}
					toolDB.rollback();
					providerDB.rollback(providerSP);
					db.close(toolDB);
					db.close(labDB);

					return false;
				}

				// 6.2 Insercción en el proveedor.
				try {

					stmtProvider.setInt(1, id);
					stmtProvider.setString(2, pname);
					stmtProvider.setString(3, pdescription);
					if (pdfl != null)
						stmtProvider.setString(4, pdfl);
					else
						stmtProvider.setNull(4, java.sql.Types.VARCHAR);
					if (pmax != null)
						stmtProvider.setString(5, pmax);
					else
						stmtProvider.setNull(5, java.sql.Types.VARCHAR);
					if (pmin != null)
						stmtProvider.setString(6, pmin);
					else
						stmtProvider.setNull(6, java.sql.Types.VARCHAR);
					stmtProvider.setString(7, pdtype);

					stmtProvider.addBatch();

				} catch (SQLException e) {
					RLF_Log.LabLog()
							.severe("[REGISTRY] Insercción del parámetro fallida en el proveedor.");
					try {
						stmtTool.close();
						stmtProvider.close();
					} catch (SQLException sqle) {
					}
					toolDB.rollback();
					providerDB.rollback(providerSP);
					db.close(toolDB);
					db.close(labDB);

					return false;
				}

				// 6.3 Creación json.
				jparameter.addProperty("name", pname);
				jparameter.addProperty("description", pdescription);
				if (pdfl != null)
					jparameter.addProperty("dfl", pdfl);
				if (pmax != null)
					jparameter.addProperty("max", pmax);
				if (pmin != null)
					jparameter.addProperty("min", pmin);
				jparameter.addProperty("data-type", pdtype);
				jparameters.add(jparameter);

			}

			// 6.4 Insercciones en la herramienta.
			try {
				stmtTool.executeBatch();
				stmtTool.close();
			} catch (SQLException e) {
				RLF_Log.LabLog()
						.severe("[REGISTRY] Insercción de parámetros fallida en la herramienta.");
				try {
					stmtTool.close();
					stmtProvider.close();
				} catch (SQLException sqle) {
				}
				toolDB.rollback();
				providerDB.rollback(providerSP);
				db.close(toolDB);
				db.close(labDB);

				return false;
			}

			// 6.5 Insercciones en el proveedor.
			try {
				stmtProvider.executeBatch();
				stmtProvider.close();
			} catch (SQLException e) {
				RLF_Log.LabLog()
						.severe("[REGISTRY] Insercción de parámetros fallida en el proveedor.");
				try {
					stmtProvider.close();
				} catch (SQLException sqle) {
				}
				toolDB.rollback();
				providerDB.rollback(providerSP);
				db.close(toolDB);
				db.close(labDB);

				return false;
			}

			// 6.6 Insercciones en json.
			jtool.add("parameters", jparameters);

			// 7. Acciones:
			node = root.getChild("actions");

			for (Object element : node.getChildren()) {

				String aname, adescription, avalue;
				int atimeout;
				Element child = (Element) element;
				jaction = new JsonObject();
				jactparams = new JsonArray();
				stmtTool = null;
				stmtProvider = null;

				try {

					stmtTool = toolDB.prepareStatement(insAction);
					stmtProvider = providerDB
							.prepareStatement(insActionProvider);

				} catch (SQLException e) {
					RLF_Log.LabLog().severe(
							"[REGISTRY] No se pueden preparar las sentencias.");
					try {
						if (stmtTool != null)
							stmtTool.close();
					} catch (SQLException sqle) {
					}
					toolDB.rollback();
					providerDB.rollback(providerSP);
					db.close(toolDB);
					db.close(labDB);
					return false;
				}

				// 7.1 Resetter:
				if (child.getName().compareToIgnoreCase("resetter") == 0) {

					aname = Tool.RESETTER;
					adescription = Tool.RESETTER;
					avalue = child.getText().replaceAll(regrex, "?");
					atimeout = 3;
					if (avalue.length() > 1024) {
						RLF_Log.LabLog()
								.warning(
										"[REGISTRY] El valor del resetter es muy largo.");
						try {
							stmtProvider.close();
							stmtTool.close();
						} catch (SQLException sqle) {
						}
						toolDB.rollback();
						providerDB.rollback(providerSP);
						db.close(toolDB);
						db.close(labDB);
						return false;
					}

					// 8. Insercción de las acciones.

					// 8.1 Insercción en la herramienta.
					try {

						stmtTool.setString(1, aname);
						stmtTool.setString(2, adescription);
						stmtTool.setString(3, avalue);
						stmtTool.setInt(4, atimeout);

						stmtTool.execute();
						stmtTool.close();

					} catch (SQLException e) {
						RLF_Log.LabLog()
								.severe("[REGISTRY] Insercción de la acción fallida en la herramienta.");
						try {
							stmtTool.close();
							stmtProvider.close();
						} catch (SQLException sqle) {
						}
						toolDB.rollback();
						providerDB.rollback(providerSP);
						db.close(toolDB);
						db.close(labDB);

						return false;
					}

					// 8.2 Insercción en el proveedor.
					try {

						stmtProvider.setInt(1, id);
						stmtProvider.setString(2, aname);
						stmtProvider.setString(3, adescription);
						stmtProvider.setInt(4, atimeout);

						stmtProvider.execute();
						stmtProvider.close();

					} catch (SQLException e) {
						RLF_Log.LabLog()
								.severe("[REGISTRY] Insercción de la acción fallida en el proveedor.");
						try {
							stmtTool.close();
							stmtProvider.close();
						} catch (SQLException sqle) {
						}
						toolDB.rollback();
						providerDB.rollback(providerSP);
						db.close(toolDB);
						db.close(labDB);

						return false;
					}

					// 8.3 Insercción en json.
					jaction.addProperty("name", aname);
					jaction.addProperty("description", adescription);
					jaction.addProperty("timeout",
							new Integer(atimeout).toString());

					// 7.2 Acción:
				} else {

					// 7.3 Nombre:
					aname = child.getAttributeValue("name").replaceAll(regrex,
							"?");
					if (aname.length() > 128) {
						RLF_Log.LabLog()
								.warning(
										"[REGISTRY] El nombre de la acción es muy largo.");
						try {
							stmtTool.close();
							stmtProvider.close();
						} catch (SQLException e) {
						}
						toolDB.rollback();
						providerDB.rollback(providerSP);
						db.close(toolDB);
						db.close(labDB);

						return false;
					}

					// 7.4 Descripción.
					adescription = child.getChild("description").getText()
							.replaceAll(regrex, "?");
					if (adescription.length() > 500) {
						RLF_Log.LabLog()
								.warning(
										"[REGISTRY] La descripción de la acción es muy larga.");
						try {
							stmtTool.close();
							stmtProvider.close();
						} catch (SQLException e) {
						}
						toolDB.rollback();
						providerDB.rollback(providerSP);
						db.close(toolDB);
						db.close(labDB);

						return false;
					}

					// 7.5 Valor.
					avalue = child.getChild("value").getText()
							.replaceAll(regrex, "?");
					if (avalue.length() > 500) {
						RLF_Log.LabLog()
								.warning(
										"[REGISTRY] El comando de la acción es muy largo.");
						try {
							stmtTool.close();
							stmtProvider.close();
						} catch (SQLException e) {
						}
						toolDB.rollback();
						providerDB.rollback(providerSP);
						db.close(toolDB);
						db.close(labDB);

						return false;
					}

					// 7.6 Timeout
					try {
						atimeout = Integer.parseInt(child.getAttributeValue(
								"timeout").replaceAll(regrex, "?"));
						if (atimeout <= 0)
							throw new Exception();
					} catch (Exception e) {
						RLF_Log.LabLog()
								.warning(
										"[REGISTRY] El tiempo máximo no tiene un formato válido.");
						try {
							stmtTool.close();
							stmtProvider.close();
						} catch (SQLException sqle) {
						}
						toolDB.rollback();
						providerDB.rollback(providerSP);
						db.close(toolDB);
						db.close(labDB);

						return false;
					}

					// 8. Insercción de las acciones.

					// 8.1 Insercción en la herramienta.
					try {

						stmtTool.setString(1, aname);
						stmtTool.setString(2, adescription);
						stmtTool.setString(3, avalue);
						stmtTool.setInt(4, atimeout);

						stmtTool.execute();
						stmtTool.close();

					} catch (SQLException e) {
						RLF_Log.LabLog()
								.severe("[REGISTRY] Insercción de la acción fallida en la herramienta.");
						try {
							stmtTool.close();
							stmtProvider.close();
						} catch (SQLException sqle) {
						}
						toolDB.rollback();
						providerDB.rollback(providerSP);
						db.close(toolDB);
						db.close(labDB);

						return false;
					}

					// 8.2 Insercción en el proveedor.
					try {

						stmtProvider.setInt(1, id);
						stmtProvider.setString(2, aname);
						stmtProvider.setString(3, adescription);
						stmtProvider.setInt(4, atimeout);

						stmtProvider.execute();
						stmtProvider.close();

					} catch (SQLException e) {
						RLF_Log.LabLog()
								.severe("[REGISTRY] Insercción de la acción fallida en el proveedor.");
						try {
							stmtTool.close();
							stmtProvider.close();
						} catch (SQLException sqle) {
						}
						toolDB.rollback();
						providerDB.rollback(providerSP);
						db.close(toolDB);
						db.close(labDB);

						return false;
					}

					// 8.3 Insercción en json.
					jaction.addProperty("name", aname);
					jaction.addProperty("description", adescription);
					jaction.addProperty("timeout",
							new Integer(atimeout).toString());

					// 9. Sockets:
					stmtTool = null;
					stmtProvider = null;

					try {

						stmtTool = toolDB.prepareStatement(insSocket);
						stmtProvider = providerDB
								.prepareStatement(insSocketProvider);

					} catch (SQLException e) {
						RLF_Log.LabLog()
								.severe("[REGISTRY] No se pueden preparar las sentencias.");
						try {
							if (stmtTool != null)
								stmtTool.close();
						} catch (SQLException sqle) {
						}
						toolDB.rollback();
						providerDB.rollback(providerSP);
						db.close(toolDB);
						db.close(labDB);
						return false;
					}

					for (Object elem : child.getChildren("socket")) {

						Element c = (Element) elem;
						jsocket = new JsonObject();

						int sport;
						String sprotocol, stype, smode;

						// 9.1 Puerto.
						try {
							sport = Integer.parseInt(c
									.getAttributeValue("port").replaceAll(
											regrex, "?"));
							if (sport <= 0)
								throw new Exception();
						} catch (Exception e) {
							RLF_Log.LabLog()
									.warning(
											"[REGISTRY] El puerto no tiene un formato válido.");
							try {
								stmtTool.close();
								stmtProvider.close();
							} catch (SQLException sqle) {
							}
							toolDB.rollback();
							providerDB.rollback(providerSP);
							db.close(toolDB);
							db.close(labDB);

							return false;
						}

						// 9.2 Protocol.
						sprotocol = c.getAttributeValue("protocol").replaceAll(
								regrex, "?");
						if (sprotocol.length() > 10) {
							RLF_Log.LabLog().warning(
									"[REGISTRY] El protocolo es muy larga.");
							try {
								stmtTool.close();
								stmtProvider.close();
							} catch (SQLException e) {
							}
							toolDB.rollback();
							providerDB.rollback(providerSP);
							db.close(toolDB);
							db.close(labDB);

							return false;
						}

						// 9.3 Type.
						stype = c.getAttributeValue("type").replaceAll(regrex,
								"?");

						// 9.4 Mode.
						smode = c.getAttributeValue("mode").replaceAll(regrex,
								"?");

						// 10. Insercción del socket.

						// 10.1 Insercción en la herramienta.
						try {

							stmtTool.setString(1, aname);
							stmtTool.setInt(2, sport);
							stmtTool.setString(3, sprotocol);
							stmtTool.setString(4, stype);
							stmtTool.setString(5, smode);

							stmtTool.addBatch();

						} catch (SQLException e) {
							RLF_Log.LabLog()
									.severe("[REGISTRY] Insercción del socket fallida en la herramienta.");
							try {
								stmtTool.close();
								stmtProvider.close();
							} catch (SQLException sqle) {
							}
							toolDB.rollback();
							providerDB.rollback(providerSP);
							db.close(toolDB);
							db.close(labDB);

							return false;
						}

						// 10.2 Insercción en el proveedor.
						try {

							stmtProvider.setInt(1, id);
							stmtProvider.setString(2, aname);
							stmtProvider.setInt(3, sport);
							stmtProvider.setString(4, sprotocol);
							stmtProvider.setString(5, stype);
							stmtProvider.setString(6, smode);

							stmtProvider.addBatch();

						} catch (SQLException e) {
							RLF_Log.LabLog()
									.severe("[REGISTRY] Insercción del socket fallida en el proveedor.");
							try {
								stmtTool.close();
								stmtProvider.close();
							} catch (SQLException sqle) {
							}
							toolDB.rollback();
							providerDB.rollback(providerSP);
							db.close(toolDB);
							db.close(labDB);

							return false;
						}

						// 10.3 Insercción en json.
						jsocket.addProperty("port",
								new Integer(sport).toString());
						jsocket.addProperty("protocol", sprotocol);
						jsocket.addProperty("type", stype);
						jsocket.addProperty("mode", smode);
						jsockets.add(jsocket);

					}

					// 10.4 Insercciones en la herramienta.
					try {
						stmtTool.executeBatch();
						stmtTool.close();
					} catch (SQLException e) {
						RLF_Log.LabLog()
								.severe("[REGISTRY] Insercción de sockets fallida en la herramienta.");
						try {
							stmtTool.close();
							stmtProvider.close();
						} catch (SQLException sqle) {
						}
						toolDB.rollback();
						providerDB.rollback(providerSP);
						db.close(toolDB);
						db.close(labDB);

						return false;
					}

					// 10.5 Insercciones en el proveedor.
					try {
						stmtProvider.executeBatch();
						stmtProvider.close();
					} catch (SQLException e) {
						RLF_Log.LabLog()
								.severe("[REGISTRY] Insercción de socket fallida en el proveedor.");
						try {
							stmtProvider.close();
						} catch (SQLException sqle) {
						}
						toolDB.rollback();
						providerDB.rollback(providerSP);
						db.close(toolDB);
						db.close(labDB);

						return false;
					}

					// 10.6 Insercciones en json.
					jaction.add("sockets", jsockets);

					// 11. Parametros:
					stmtTool = null;
					stmtProvider = null;

					try {

						stmtTool = toolDB.prepareStatement(insActParameter);
						stmtProvider = providerDB
								.prepareStatement(insActParameterProvider);

					} catch (SQLException e) {
						RLF_Log.LabLog()
								.severe("[REGISTRY] No se pueden preparar las sentencias.");
						try {
							if (stmtTool != null)
								stmtTool.close();
						} catch (SQLException sqle) {
						}
						toolDB.rollback();
						providerDB.rollback(providerSP);
						db.close(toolDB);
						db.close(labDB);
						return false;
					}

					for (Object elem : child.getChildren("action_parameter")) {

						Element c = (Element) elem;
						jactparam = new JsonObject();
						String apname, aptype;

						// 11.1 Nombre:
						apname = c.getAttributeValue("name").replaceAll(regrex,
								"?");
						if (!params.contains(apname)) {
							RLF_Log.LabLog()
									.warning(
											"[REGISTRY] El parámetro en la acción no corresponde con un parámetro válido.");
							try {
								stmtProvider.close();
							} catch (SQLException sqle) {
							}
							toolDB.rollback();
							providerDB.rollback(providerSP);
							db.close(toolDB);
							db.close(labDB);

							return false;
						}

						// 11.2 Type:
						aptype = c.getAttributeValue("type");

						// 12. Insercción del parámetro.

						// 12.1 Insercción en la herramienta.
						try {

							stmtTool.setString(1, aname);
							stmtTool.setString(2, apname);
							stmtTool.setString(3, aptype);

							stmtTool.addBatch();

						} catch (SQLException e) {
							RLF_Log.LabLog()
									.severe("[REGISTRY] Insercción del parámetro fallida en la herramienta.");
							try {
								stmtTool.close();
								stmtProvider.close();
							} catch (SQLException sqle) {
							}
							toolDB.rollback();
							providerDB.rollback(providerSP);
							db.close(toolDB);
							db.close(labDB);

							return false;
						}

						// 12.2 Insercción en el proveedor.
						try {

							stmtProvider.setInt(1, id);
							stmtProvider.setString(2, aname);
							stmtProvider.setString(3, apname);
							stmtProvider.setString(4, aptype);

							stmtProvider.addBatch();

						} catch (SQLException e) {
							RLF_Log.LabLog()
									.severe("[REGISTRY] Insercción del parámetro fallida en el proveedor.");
							try {
								stmtTool.close();
								stmtProvider.close();
							} catch (SQLException sqle) {
							}
							toolDB.rollback();
							providerDB.rollback(providerSP);
							db.close(toolDB);
							db.close(labDB);

							return false;
						}

						// 12.3 Insercción en json.
						jactparam.addProperty("name", apname);
						jactparam.addProperty("type", aptype);
						jactparams.add(jactparam);

					}

					// 12.4 Insercciones en la herramienta.
					try {
						stmtTool.executeBatch();
						stmtTool.close();
					} catch (SQLException e) {
						RLF_Log.LabLog()
								.severe("[REGISTRY] Insercción de parámetros fallida en la herramienta.");
						try {
							stmtTool.close();
							stmtProvider.close();
						} catch (SQLException sqle) {
						}
						toolDB.rollback();
						providerDB.rollback(providerSP);
						db.close(toolDB);
						db.close(labDB);

						return false;
					}

					// 12.5 Insercciones en el proveedor.
					try {
						stmtProvider.executeBatch();
						stmtProvider.close();
					} catch (SQLException e) {
						RLF_Log.LabLog()
								.severe("[REGISTRY] Insercción de parámetros fallida en el proveedor.");
						try {
							stmtProvider.close();
						} catch (SQLException sqle) {
						}
						toolDB.rollback();
						providerDB.rollback(providerSP);
						db.close(toolDB);
						db.close(labDB);

						return false;
					}

					// 12.6 Insercciones en json.
					jaction.add("parameters", jactparams);

				}

				jactions.add(jaction);

			}

		} else {

			// 7.1 Action:
			String aname, adescription, avalue;
			int atimeout;
			Element child = root.getChild("action");
			jaction = new JsonObject();
			stmtTool = null;
			stmtProvider = null;

			try {

				stmtTool = toolDB.prepareStatement(insAction);
				stmtProvider = providerDB.prepareStatement(insActionProvider);

			} catch (SQLException e) {
				RLF_Log.LabLog().severe(
						"[REGISTRY] No se pueden preparar las sentencias.");
				try {
					if (stmtTool != null)
						stmtTool.close();
				} catch (SQLException sqle) {
				}
				toolDB.rollback();
				providerDB.rollback(providerSP);
				db.close(toolDB);
				db.close(labDB);
				return false;
			}

			// 7.2 Nombre:
			aname = child.getAttributeValue("name").replaceAll(regrex, "?");
			if (aname.length() > 128) {
				RLF_Log.LabLog().warning(
						"[REGISTRY] El nombre de la acción es muy largo.");
				try {
					stmtTool.close();
					stmtProvider.close();
				} catch (SQLException e) {
				}
				toolDB.rollback();
				providerDB.rollback(providerSP);
				db.close(toolDB);
				db.close(labDB);

				return false;
			}

			// 7.3 Descripción.
			adescription = child.getChild("description").getText()
					.replaceAll(regrex, "?");
			if (adescription.length() > 500) {
				RLF_Log.LabLog().warning(
						"[REGISTRY] La descripción de la acción es muy larga.");
				try {
					stmtTool.close();
					stmtProvider.close();
				} catch (SQLException e) {
				}
				toolDB.rollback();
				providerDB.rollback(providerSP);
				db.close(toolDB);
				db.close(labDB);

				return false;
			}

			// 7.4 Valor.
			avalue = child.getChild("value").getText().replaceAll(regrex, "?");
			if (avalue.length() > 500) {
				RLF_Log.LabLog().warning(
						"[REGISTRY] El comando de la acción es muy largo.");
				try {
					stmtTool.close();
					stmtProvider.close();
				} catch (SQLException e) {
				}
				toolDB.rollback();
				providerDB.rollback(providerSP);
				db.close(toolDB);
				db.close(labDB);

				return false;
			}

			// 7.5 Timeout
			try {
				atimeout = Integer.parseInt(child.getAttributeValue("timeout")
						.replaceAll(regrex, "?"));
				if (atimeout <= 0)
					throw new Exception();
			} catch (Exception e) {
				RLF_Log.LabLog()
						.warning(
								"[REGISTRY] El tiempo máximo no tiene un formato válido.");
				try {
					stmtTool.close();
					stmtProvider.close();
				} catch (SQLException sqle) {
				}
				toolDB.rollback();
				providerDB.rollback(providerSP);
				db.close(toolDB);
				db.close(labDB);

				return false;
			}

			// 8. Insercción de las acciones.

			// 8.1 Insercción en la herramienta.
			try {

				stmtTool.setString(1, aname);
				stmtTool.setString(2, adescription);
				stmtTool.setString(3, avalue);
				stmtTool.setInt(4, atimeout);

				stmtTool.execute();
				stmtTool.close();

			} catch (SQLException e) {
				RLF_Log.LabLog()
						.severe("[REGISTRY] Insercción de la acción fallida en la herramienta.");
				try {
					stmtTool.close();
					stmtProvider.close();
				} catch (SQLException sqle) {
				}
				toolDB.rollback();
				providerDB.rollback(providerSP);
				db.close(toolDB);
				db.close(labDB);

				return false;
			}

			// 8.2 Insercción en el proveedor.
			try {

				stmtProvider.setInt(1, id);
				stmtProvider.setString(2, aname);
				stmtProvider.setString(3, adescription);
				stmtProvider.setInt(4, atimeout);

				stmtProvider.execute();
				stmtProvider.close();

			} catch (SQLException e) {
				RLF_Log.LabLog()
						.severe("[REGISTRY] Insercción de la acción fallida en el proveedor.");
				try {
					stmtTool.close();
					stmtProvider.close();
				} catch (SQLException sqle) {
				}
				toolDB.rollback();
				providerDB.rollback(providerSP);
				db.close(toolDB);
				db.close(labDB);

				return false;
			}

			// 8.3 Insercción en json.
			jaction.addProperty("name", aname);
			jaction.addProperty("description", adescription);
			jaction.addProperty("timeout", new Integer(atimeout).toString());

			// 9. Sockets:
			stmtTool = null;
			stmtProvider = null;

			try {

				stmtTool = toolDB.prepareStatement(insSocket);
				stmtProvider = providerDB.prepareStatement(insSocketProvider);

			} catch (SQLException e) {
				RLF_Log.LabLog().severe(
						"[REGISTRY] No se pueden preparar las sentencias.");
				try {
					if (stmtTool != null)
						stmtTool.close();
				} catch (SQLException sqle) {
				}
				toolDB.rollback();
				providerDB.rollback(providerSP);
				db.close(toolDB);
				db.close(labDB);
				return false;
			}

			for (Object elem : child.getChildren("socket")) {

				Element c = (Element) elem;
				jsocket = new JsonObject();

				int sport;
				String sprotocol, stype, smode;

				// 9.1 Puerto.
				try {
					sport = Integer.parseInt(c.getAttributeValue("port")
							.replaceAll(regrex, "?"));
					if (sport <= 0)
						throw new Exception();
				} catch (Exception e) {
					RLF_Log.LabLog().warning(
							"[REGISTRY] El puerto no tiene un formato válido.");
					try {
						stmtTool.close();
						stmtProvider.close();
					} catch (SQLException sqle) {
					}
					toolDB.rollback();
					providerDB.rollback(providerSP);
					db.close(toolDB);
					db.close(labDB);

					return false;
				}

				// 9.2 Protocol.
				sprotocol = c.getAttributeValue("protocol").replaceAll(regrex,
						"?");
				if (sprotocol.length() > 500) {
					RLF_Log.LabLog().warning(
							"[REGISTRY] El protocolo es muy larga.");
					try {
						stmtTool.close();
						stmtProvider.close();
					} catch (SQLException e) {
					}
					toolDB.rollback();
					providerDB.rollback(providerSP);
					db.close(toolDB);
					db.close(labDB);

					return false;
				}

				// 9.3 Type.
				stype = c.getAttributeValue("type").replaceAll(regrex, "?");

				// 9.4 Mode.
				smode = c.getAttributeValue("mode").replaceAll(regrex, "?");

				// 10. Insercción del socket.

				// 10.1 Insercción en la herramienta.
				try {

					stmtTool.setString(1, aname);
					stmtTool.setInt(2, sport);
					stmtTool.setString(3, sprotocol);
					stmtTool.setString(4, stype);
					stmtTool.setString(5, smode);

					stmtTool.addBatch();

				} catch (SQLException e) {
					RLF_Log.LabLog()
							.severe("[REGISTRY] Insercción del socket fallida en la herramienta.");
					try {
						stmtTool.close();
						stmtProvider.close();
					} catch (SQLException sqle) {
					}
					toolDB.rollback();
					providerDB.rollback(providerSP);
					db.close(toolDB);
					db.close(labDB);

					return false;
				}

				// 10.2 Insercción en el proveedor.
				try {

					stmtProvider.setInt(1, id);
					stmtProvider.setString(2, aname);
					stmtProvider.setInt(3, sport);
					stmtProvider.setString(4, sprotocol);
					stmtProvider.setString(5, stype);
					stmtProvider.setString(6, smode);

					stmtProvider.addBatch();

				} catch (SQLException e) {
					RLF_Log.LabLog()
							.severe("[REGISTRY] Insercción del socket fallida en el proveedor.");
					try {
						stmtTool.close();
						stmtProvider.close();
					} catch (SQLException sqle) {
					}
					toolDB.rollback();
					providerDB.rollback(providerSP);
					db.close(toolDB);
					db.close(labDB);

					return false;
				}

				// 10.3 Insercción en json.
				jsocket.addProperty("port", new Integer(sport).toString());
				jsocket.addProperty("protocol", sprotocol);
				jsocket.addProperty("type", stype);
				jsocket.addProperty("mode", smode);
				jsockets.add(jsocket);

			}

			// 10.4 Insercciones en la herramienta.
			try {
				stmtTool.executeBatch();
				stmtTool.close();
			} catch (SQLException e) {
				RLF_Log.LabLog()
						.severe("[REGISTRY] Insercción de sockets fallida en la herramienta.");
				try {
					stmtTool.close();
					stmtProvider.close();
				} catch (SQLException sqle) {
				}
				toolDB.rollback();
				providerDB.rollback(providerSP);
				db.close(toolDB);
				db.close(labDB);

				return false;
			}

			// 10.5 Insercciones en el proveedor.
			try {
				stmtProvider.executeBatch();
				stmtProvider.close();
			} catch (SQLException e) {
				RLF_Log.LabLog()
						.severe("[REGISTRY] Insercción de socket fallida en el proveedor.");
				try {
					stmtProvider.close();
				} catch (SQLException sqle) {
				}
				toolDB.rollback();
				providerDB.rollback(providerSP);
				db.close(toolDB);
				db.close(labDB);

				return false;
			}

			// 10.6 Insercciones en json.
			jaction.add("sockets", jsockets);
			jactions.add(jaction);

			// 11 Resetter:
			child = root.getChild("resetter");
			jaction = new JsonObject();

			try {

				stmtTool = toolDB.prepareStatement(insAction);
				stmtProvider = providerDB.prepareStatement(insActionProvider);

			} catch (SQLException e) {
				RLF_Log.LabLog().severe(
						"[REGISTRY] No se pueden preparar las sentencias.");
				try {
					if (stmtTool != null)
						stmtTool.close();
				} catch (SQLException sqle) {
				}
				toolDB.rollback();
				providerDB.rollback(providerSP);
				db.close(toolDB);
				db.close(labDB);
				return false;
			}

			// 11.1 Valores.
			aname = Tool.RESETTER;
			adescription = Tool.RESETTER;
			avalue = child.getText().replaceAll(regrex, "?");
			atimeout = 3;
			if (avalue.length() > 1024) {
				RLF_Log.LabLog().warning(
						"[REGISTRY] El valor del resetter es muy largo.");
				try {
					stmtProvider.close();
					stmtTool.close();
				} catch (SQLException sqle) {
				}
				toolDB.rollback();
				providerDB.rollback(providerSP);
				db.close(toolDB);
				db.close(labDB);
				return false;
			}

			// 12. Insercción del reseteador.

			// 12.1 Insercción en la herramienta.
			try {

				stmtTool.setString(1, aname);
				stmtTool.setString(2, adescription);
				stmtTool.setString(3, avalue);
				stmtTool.setInt(4, atimeout);

				stmtTool.execute();
				stmtTool.close();

			} catch (SQLException e) {
				RLF_Log.LabLog()
						.severe("[REGISTRY] Insercción de la acción fallida en la herramienta.");
				try {
					stmtTool.close();
					stmtProvider.close();
				} catch (SQLException sqle) {
				}
				toolDB.rollback();
				providerDB.rollback(providerSP);
				db.close(toolDB);
				db.close(labDB);

				return false;
			}

			// 12.2 Insercción en el proveedor.
			try {

				stmtProvider.setInt(1, id);
				stmtProvider.setString(2, aname);
				stmtProvider.setString(3, adescription);
				stmtProvider.setInt(4, atimeout);

				stmtProvider.execute();
				stmtProvider.close();

			} catch (SQLException e) {
				RLF_Log.LabLog()
						.severe("[REGISTRY] Insercción de la acción fallida en el proveedor.");
				try {
					stmtTool.close();
					stmtProvider.close();
				} catch (SQLException sqle) {
				}
				toolDB.rollback();
				providerDB.rollback(providerSP);
				db.close(toolDB);
				db.close(labDB);

				return false;
			}

			// 12.3 Insercción en json.
			jaction.addProperty("name", aname);
			jaction.addProperty("description", adescription);
			jaction.addProperty("timeout", new Integer(atimeout).toString());

			jactions.add(jaction);
		}

		jtool.add("actions", jactions);

		// 13. Insercción en el laboratorio.
		stmtLab = null;
		try {
			stmtLab = labDB.prepareStatement(insToolLab);

			stmtLab.setInt(1, id);
			stmtLab.setString(2, path);
			stmtLab.setString(3, key);

			stmtLab.execute();
			stmtLab.close();

		} catch (SQLException e) {
			RLF_Log.LabLog()
					.severe("[REGISTRY] Insercción de la herramienta en el laboratorio fallida.");
			try {
				if (stmtLab != null)
					stmtLab.close();
			} catch (SQLException sqle) {
			}
			toolDB.rollback();
			providerDB.rollback(providerSP);
			db.close(toolDB);
			db.close(labDB);
		}

		// 14. Modificación de la herramienta para incluir el json.
		stmtProvider = null;
		try {
			stmtProvider = providerDB.prepareStatement(updTool);

			stmtProvider.setString(1, new Gson().toJson(jtool));
			stmtProvider.setInt(2, id);

			stmtProvider.execute();
			stmtProvider.close();

		} catch (SQLException e) {
			RLF_Log.LabLog()
					.severe("[REGISTRY] Fallo al actualizar la herramienta en el proveedor.");
			try {
				if (stmtProvider != null)
					stmtProvider.close();
			} catch (SQLException sqle) {
			}
			toolDB.rollback();
			providerDB.rollback(providerSP);
			labDB.rollback();
			db.close(toolDB);
			db.close(labDB);
		}

		// 15. Envío de datos.
		try {

			labDB.commit();
			toolDB.commit();
			providerDB.commit();
			providerDB.setAutoCommit(true);

		} catch (SQLException e) {
			RLF_Log.LabLog()
					.severe("[REGISTRY] Fallo al actualizar la herramienta en el proveedor.");
			try {
				if (stmtProvider != null)
					stmtProvider.close();
			} catch (SQLException sqle) {
			}
			toolDB.rollback();
			providerDB.rollback(providerSP);
			labDB.rollback();
			db.close(toolDB);
			db.close(labDB);
		}

		return true;
	}

	/**
	 * Elimina una herramienta del laboratorio. Borra la base de datos de la
	 * herramienta y los datos almacenados. No lo borra del directorio. Si está
	 * activo no se ejecutará.
	 * 
	 * @param id
	 *            Identificador de la herramienta.
	 * @param key
	 *            Clave asignada a la herramienta.
	 * @return Verdadero si ha podido eliminarse, falso si no existía.
	 */
	public boolean drop(int id, String key) {

		if (this.context.isArmed())
			return false;

		// 1. Eliminación de la información de la base de datos del laboratorio.
		DBHelper db = new DBHelper();
		Connection lab = db.connect();
		String sel = "SELECT path FROM tool WHERE id = ? AND key = ?";
		String del = "DELETE FROM tool WHERE id = ?";
		String path = null;
		if (lab == null) {
			RLF_Log.LabLog()
					.severe("[DROP] No se puede conectar con la base de datos del laboratorio.");
			return false;
		}

		try {

			PreparedStatement stmt = lab.prepareStatement(sel);
			stmt.setInt(1, id);
			stmt.setString(2, key);
			ResultSet rs = stmt.executeQuery();
			if (!rs.next()){
				RLF_Log.LabLog().warning("[DROP] No existe esa herramienta o la clave no es válida.");
				rs.close();
				stmt.close();
				db.close(lab);
				return false;
			} else {
				path = rs.getString("path");
				rs.close();
				stmt.close();
			}
			
			stmt = lab.prepareStatement(del);
			stmt.setInt(1, id);
			stmt.executeUpdate();
			stmt.close();

		} catch (SQLException e) {
			db.close(lab);
			return false;
		}

		db.close(lab);

		// 2. Eliminación de la base de datos.
		db.deleteDB(path);

		return true;
	}

	/**
	 * Elimina de memoria principal todas las herramientas. Se utiliza para
	 * cuando se quiere activar el laboratorio, para comprobar las herramientas
	 * nuevas.
	 */
	public void reset() {
		this.tools.clear();
	}

}
