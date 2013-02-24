/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rlf.provider.ws;

import com.google.gson.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.logging.Level;
import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import org.rlf.cipher.RLF_Cipher;
import org.rlf.cipher.dummy.RLF_DummyCipher;
import org.rlf.log.RLF_Log;
import org.rlf.net.RLF_NetHelper;
import org.rlf.net.message.*;
import org.rlf.provider.ws.data.Database;
import org.rlf.provider.ws.data.Lab;
import org.rlf.provider.ws.exception.*;

/**
 * Implementación del servicio web que responde a las peticiones de los
 * clientes. Cada petición conecta con la base de datos y/o realiza peticiones
 * a los laboratorios.
 * 
 * @author Carlos A. Rodríguez Mecha
 * @version 0.1
 */
@WebService(serviceName = "Provider")
public class Provider {

    /**
     * Operación de acceso con un usuario y una contraseña. El usuario debe
     * estar dado de alta como cliente de RLF en el proveedor. Se devuelve un
     * token de autentificación por el que poder realizar más peticiones. Este
     * token no tiene caducidad.
     * 
     * @param user Usuario.
     * @param hash_pass Hash de la contraseña del usuario.
     * @return Token de acceso permamente.
     * @throws AuthException Fallo de autentificación. Puede ser que el usuario
     *                       ya estuviera logueado.
     * @throws ConnectionException Fallo de conexión con los módulos de RLF. 
     */
    @WebMethod(operationName = "login")
    public String login(@WebParam(name = "user") String user,
            @WebParam(name = "hash_pass") String hash_pass)
            throws AuthException, ConnectionException {

        String auth_token = null;
        Database db = new Database();
        RLF_Cipher cipher = new RLF_DummyCipher();
        Connection conn = db.connect();
        if (conn == null) {
            RLF_Log.ProviderLog().severe("[LOGIN] Operación de login cancelada.");
            throw new ConnectionException();
        }

        try {

            // 1. Verificación del usuario.
            CallableStatement stmt = conn.prepareCall("{CALL loginClient(?)}");
            stmt.setString("client_hash", cipher.getHash(user + hash_pass));
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                // Fallo de autentificación.
                rs.close();
                stmt.close();
                db.disconnect(conn);
                throw new AuthException();
            }

            auth_token = rs.getString("auth");
            rs.close();
            stmt.close();
            db.disconnect(conn);

        } catch (SQLException sqle) {
            RLF_Log.ProviderLog().log(Level.SEVERE, "[LOGIN] Problema de comunicación "
                    + "con la base de datos: {0}", sqle.getMessage());
            db.disconnect(conn);
            throw new ConnectionException();
        }

        return auth_token;
    }

    /**
     * Indica al proveedor que el cliente se desconecta. Se elimina su token
     * de acceso y su token de uso de herramientas. Después, se avisa a los
     * laboratorios que corresponden.
     * 
     * @param auth Token de acceso permamente.
     * @return Verdadero.
     * @throws AuthException Fallo de autentificación.
     * @throws ConnectionException Fallo de conexión con los módulos de RLF.
     */
    @WebMethod(operationName = "logout")
    public Boolean logout(@WebParam(name = "auth") String auth)
            throws AuthException, ConnectionException {

        Database db = new Database();
        RLF_NetHelper net = new RLF_NetHelper();
        
        
        CallableStatement s = null;
        ResultSet rs = null;
        String host;
        int port;
        SocketChannel socket = null;
        String name;
        Connection conn = db.connect();
        if (conn == null) {
            RLF_Log.ProviderLog().severe("[LOGOUT] Operación de logout cancelada.");
            throw new ConnectionException();
        }

        // 1. Verificación del token.
        name = authUser(conn, auth);
        if (name == null) {
            db.disconnect(conn);
            throw new AuthException();
        }

        try {

            // 2. Notificación a los laboratorios.
            s = conn.prepareCall("{CALL usedTools(?, ?)}");
            s.setString("client_name", name);
            s.registerOutParameter("useToken", java.sql.Types.VARCHAR);
            rs = s.executeQuery();
            LinkedList<String> hosts = new LinkedList<String>();
            
            while (rs.next()) {
                HashMap<String, String> attributes = new HashMap<String, String>();
                attributes.put("token", s.getString("useToken"));
                host = rs.getString("host");
                if (hosts.contains(host)) continue;
                port = rs.getInt("port");
                socket = SocketChannel.open(new InetSocketAddress(host, port));
                RLF_NetMessage msg = new RLF_NetMessage(RLF_NetMessageID.LOGOUT, attributes);
                if (net.sendMessage(msg, socket)) {
                    net.reciveMessage(socket);
                }
                socket.close();
                hosts.add(host);
            }

            // 3. Eliminación del token.
            s = conn.prepareCall("{CALL logoutClient(?)}");
            s.setString("client_name", name);
            s.execute();
            s.close();
            db.disconnect(conn);

        } catch (IOException ex) {
            RLF_Log.ProviderLog().log(Level.SEVERE, "Problema de comunicación "
                    + "con la base de datos: {0}", ex.getMessage());
            try {
                rs.close();
                s.close();
                socket.close();
            } catch (Exception e) {
            }
            db.disconnect(conn);
            throw new ConnectionException();
        } catch (SQLException sqle) {
            RLF_Log.ProviderLog().log(Level.SEVERE, "Problema de comunicación "
                    + "con la base de datos: {0}", sqle.getMessage());
            try {
                rs.close();
                s.close();
            } catch (Exception e) {
            }
            db.disconnect(conn);

            throw new ConnectionException();
        }

        return true;
    }

    /**
     * Obtiene los estados de las herramientas que pueden ser utilizadas por 
     * el usuario que realiza la petición. El estado puede ser 'free' si no
     * está siendo utilizada, 'in use' si algún usuario la está usando y
     * 'tooldata' si es una herramienta que no requiere ser reservada.
     * 
     * @param auth Token de acceso.
     * @return Array en formato Json con objetos de forma (id de la herramienta,
     *         estado). [{"id":"...", "name":"...", "status":"..."}, ...]
     * @throws AuthException Fallo de autentificación.
     * @throws ConnectionException Fallo de conexión con los módulos de RLF.
     */
    @WebMethod(operationName = "getStatus")
    public String getStatus(@WebParam(name = "auth") String auth)
            throws AuthException, ConnectionException {

        JsonArray array = new JsonArray();
        Database db = new Database();
        String name;
        CallableStatement s = null;
        ResultSet rs = null;
        Connection conn = db.connect();
        if (conn == null) {
            RLF_Log.ProviderLog().severe("[STATUS] Operación de estado cancelada.");
            throw new ConnectionException();
        }

        // 1. Verificación del token.
        name = authUser(conn, auth);
        if (name == null) {
            db.disconnect(conn);
            throw new AuthException();
        }

        try {

            // 2. Obtención de los servicios.
            s = conn.prepareCall("{CALL status(?)}");
            s.setString("client_name", name);
            rs = s.executeQuery();
            while (rs.next()) {

                JsonObject tool = new JsonObject();
                String id = new Integer(rs.getInt("id")).toString();
                String toolname = rs.getString("name");
                String status = rs.getString("status");
                tool.addProperty("id", id);
                tool.addProperty("name", toolname);
                tool.addProperty("status", status);
                array.add(tool);

            }

            s.close();
            rs.close();
            db.disconnect(conn);

        } catch (SQLException sqle) {
            RLF_Log.ProviderLog().log(Level.SEVERE, "[STATUS] Problema de comunicación "
                    + "con la base de datos: {0}", sqle.getMessage());
            try {
                rs.close();
                s.close();
            } catch (Exception e) {
            }
            db.disconnect(conn);
            throw new ConnectionException();
        }

        return new Gson().toJson(array);
    }

    /**
     * Reserva las herramientas especificadas para su uso. Estas herramientas
     * se podrán utilizar durante el tiempo máximo especificado en la creación
     * del usuario. Después de ese tiempo, se notificará a los clientes que se
     * ha termiado. El objeto devuelto contiene el token de uso de las
     * herramientas y cómo se puede acceder a la herramienta correspondiente.
     * 
     * @param auth Token de acceso.
     * @param array Array en formato Json con los identificadores numéricos.
     *              [id, id, ...]
     * @return Objeto Json que contiene el token de uso de las herramientas
     *         el cual se deberá añadir a los mensajes enviados al laboratorio
     *         así como la dirección y puerto de cada laboratorio con el
     *         identificador de la herramienta correspondiente.
     *         {"token":"...", "timeout":"...", "labs":[{"id":"...", "host":"...", "port":"...", "notification":"..."}, ...]}
     * @throws AuthException El token de acceso no es correcto.
     * @throws ConnectionException Fallo de conexión con los módulos de RLF.
     * @throws ToolException Alguna de las herramientas ya está reservada o no 
     *                       tiene permiso para usarla.
     * @throws FormatException El array de entrada tiene un formato incorrecto.
     */
    @WebMethod(operationName = "takeTools")
    public String takeTools(@WebParam(name = "auth") String auth,
            @WebParam(name = "array") String array)
            throws AuthException, ConnectionException, ToolException,
            FormatException {

        JsonObject reply = new JsonObject();
        JsonArray labs = new JsonArray(), ids;
        JsonParser parser = new JsonParser();
        String name, token;
        Savepoint sp = null;
        CallableStatement s = null;
        ResultSet rs = null;
        HashMap<String, Lab> labsInfo = new HashMap<String, Lab>();
        LinkedList<Integer> tools = new LinkedList<Integer>();
        LinkedList<Integer> request = new LinkedList<Integer>();
        RLF_NetHelper net = new RLF_NetHelper();
        RLF_NetMessage msg = new RLF_NetMessage(), out;
        HashMap<String, String> attributes = new HashMap<String, String>();
        SocketChannel socket = null;
        int timeout;

        // 1. Autentificación del usuario.
        Database db = new Database();
        Connection conn = db.connect();
        if (conn == null) {
            RLF_Log.ProviderLog().severe("[TAKE] Operación de reservar cancelada.");
            throw new ConnectionException();
        }

        // 1. Verificación del token.
        name = authUser(conn, auth);
        if (name == null) {
            db.disconnect(conn);
            throw new AuthException();
        }

        // 2. Verificación del array.
        try {
            ids = parser.parse(array).getAsJsonArray();
            for (JsonElement e : ids) {
                request.add(e.getAsInt());
            }
        } catch (Exception e) {
            db.disconnect(conn);
            throw new FormatException();
        }

        try {

            synchronized (this) {
                // 3. Obtención de las herramientas.
                s = conn.prepareCall("{CALL status(?)}");
                s.setString("client_name", name);
                rs = s.executeQuery();
                while (rs.next()) {
                    if (rs.getString("status").compareToIgnoreCase("ONLINE") == 0) {
                        tools.add(rs.getInt("id"));
                    }
                }
                rs.close();
                s.close();

                // 4. Verificación de disponibilidad.
                for (Integer id : request) {
                    if (!tools.contains(id)) {
                        db.disconnect(conn);
                        throw new ToolException();
                    }
                }

                conn.setAutoCommit(false);
                sp = conn.setSavepoint();

                // 5. Registro.
                s = conn.prepareCall("{CALL taketool(?, ?)}");
                for (Integer id : request) {
                    s.setString("client_name", name);
                    s.setInt("id_tool", id);
                    rs = s.executeQuery();
                    if (!rs.next()) {
                        rs.close();
                        s.close();
                        conn.rollback(sp);
                        db.disconnect(conn);
                        throw new ToolException();
                    }
                    Lab lab = new Lab(rs.getString("name"), rs.getString("host"),
                            rs.getInt("port"), rs.getInt("pclient"),
                            rs.getInt("pnotification"));
                    if (labsInfo.containsKey(lab.getName())) {
                        labsInfo.get(lab.getName()).addTool(id);
                    } else {
                        lab.addTool(id);
                        labsInfo.put(lab.getName(), lab);
                    }
                }
            }
            // 7. Petición del token.
            s = conn.prepareCall("{CALL usetoken(?)}");
            s.setString("client_name", name);
            rs = s.executeQuery();
            rs.next();
            token = rs.getString("token");
            timeout = rs.getInt("timeout");
            rs.close();
            s.close();

            // 8. Notificación a los laboratorios.
            for (Entry<String, Lab> e : labsInfo.entrySet()) {
                Lab lab = e.getValue();
                attributes = new HashMap<String, String>();
                attributes.put("token", token);
                attributes.put("timeout", new Integer(timeout).toString());
                JsonArray a = new JsonArray();
                for (Integer i : e.getValue().getTools()) {
                    a.add(new JsonPrimitive(i));
                }
                attributes.put("tools", new Gson().toJson(a));
                msg = new RLF_NetMessage(RLF_NetMessageID.TOKEN, attributes);
                socket = SocketChannel.open(new InetSocketAddress(lab.getHost(), lab.getPort()));
                if (!net.sendMessage(msg, socket)) {
                    socket.close();
                    conn.rollback(sp);
                    db.disconnect(conn);
                    throw new ConnectionException();
                }
                out = net.reciveMessage(socket);
                if (out == null || out.getId() != RLF_NetMessageID.OK) {
                    socket.close();
                    conn.rollback(sp);
                    db.disconnect(conn);
                    throw new ToolException();
                }
                socket.close();

            }

            // 9. Afianzamiento.
            conn.commit();
            db.disconnect(conn);

        } catch (IOException ex) {
            RLF_Log.ProviderLog().log(Level.SEVERE, "[TAKE] Problema de comunicación "
                    + "con los laboratorios: {0}", ex.getMessage());
            try {
                rs.close();
                s.close();
                conn.rollback(sp);
                socket.close();
            } catch (Exception e) {
            }

            db.disconnect(conn);
            throw new ConnectionException();
        } catch (SQLException sqle) {
            RLF_Log.ProviderLog().log(Level.SEVERE, "[TAKE] Problema de comunicación "
                    + "con la base de datos: {0}", sqle.getMessage());
            try {
                rs.close();
                s.close();
                conn.rollback(sp);
            } catch (Exception e) {
            }
            db.disconnect(conn);
            throw new ConnectionException();
        }

        // 10. Creación del objeto a enviar.
        reply.addProperty("token", token);
        reply.addProperty("timeout", timeout);
        for (Lab lab : labsInfo.values()) {
            JsonObject jlab = lab.toJson();
            labs.add(jlab);
        }
        reply.add("labs", labs);

        return new Gson().toJson(reply);
    }

    /**
     * Obtiene la descripción en formato Json de las herramientas a las que el
     * usuario puede acceder. Para saber el formato mirar la documentación.
     * @param auth Token de acceso.
     * @return Array con las herramientas en formato json.
     * @throws ConnectionException Problema con la comunicación entre módulos RLF.
     * @throws AuthException Problema de autentificación.
     */
    @WebMethod(operationName = "describeTools")
    public String describeTools(@WebParam(name = "auth") String auth) throws ConnectionException, AuthException {

        JsonArray array = new JsonArray();
        JsonParser parser = new JsonParser();
        Database db = new Database();
        String name;
        CallableStatement s = null;
        ResultSet rs = null;
        Connection conn = db.connect();
        if (conn == null) {
            RLF_Log.ProviderLog().severe("[DESCRIBE] Operación de descripción cancelada.");
            throw new AuthException();
        }

        // 1. Verificación del token.
        name = authUser(conn, auth);
        if (name == null) {
            db.disconnect(conn);
            throw new AuthException();
        }

        try {

            // 2. Obtención de los servicios.
            s = conn.prepareCall("{CALL definitions(?)}");
            s.setString("client_name", name);
            rs = s.executeQuery();
            while (rs.next()) {

                JsonObject tool = new JsonObject();
                try {
                    tool = parser.parse(rs.getString("json")).getAsJsonObject();
                } catch (Exception e) {
                    continue;
                }
                array.add(tool);

            }

            s.close();
            rs.close();
            db.disconnect(conn);

        } catch (SQLException sqle) {
            RLF_Log.ProviderLog().log(Level.SEVERE, "[DESCRIBE] Problema de comunicación "
                    + "con la base de datos: {0}", sqle.getMessage());
            try {
                rs.close();
                s.close();
            } catch (Exception e) {
            }
            db.disconnect(conn);
            throw new ConnectionException();
        }

        return new Gson().toJson(array);
    }

    /**
     * Autentifica a un usuario por su token de acceso.
     * @param conn Conexión abierta con la base de datos.
     * @param auth Token de acceso proporcionado por el servicio web.
     * @return Nombre del usuario, null si hay un fallo de autentificación.
     */
    private String authUser(Connection conn, String auth) {

        String name = null;

        try {

            // 1. Verificación del token.
            CallableStatement s = conn.prepareCall("{CALL verifytoken(?)}");
            s.setString("client_auth", auth);
            ResultSet rs = s.executeQuery();

            if (!rs.next()) {
                // Fallo de autentificación.
                rs.close();
                s.close();
                return null;
            }

            name = rs.getString("c.name");
            rs.close();
            s.close();

        } catch (SQLException sqle) {
            RLF_Log.ProviderLog().log(Level.SEVERE, "[AUTH] Problema de comunicación "
                    + "con la base de datos: {0}", sqle.getMessage());
            return null;
        }

        return name;

    }
}

