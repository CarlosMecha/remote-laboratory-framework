/*
 * Monitor.
 */
package org.rlf.monitor.ws;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import org.rlf.cipher.RLF_Cipher;
import org.rlf.cipher.dummy.RLF_DummyCipher;
import org.rlf.log.RLF_Log;
import org.rlf.provider.ws.data.Database;
import org.rlf.provider.ws.exception.AuthException;
import org.rlf.provider.ws.exception.ConnectionException;
import org.rlf.monitor.ws.ToolInfo.ToolStatus;

/**
 * Servicio web para la monitorización del estado de las herramientas.
 * 
 * @author Carlos A. Rodríguez Mecha
 * @version 0.1
 */
@WebService(serviceName = "Monitor")
public class Monitor {

    /**
     * Acceso por parte de un monitor de un cliente al provedor.
     * @param user Usuario.
     * @param hash_pass Hash de la contraseña del usuario.
     * @return Token de acceso permamente para el monitor.
     * @throws AuthException Fallo de autentificación. Puede ser que el monitor
     *                       ya estuviera logueado.
     * @throws ConnectionException Fallo de conexión con los módulos de RLF. 
     */
    @WebMethod(operationName = "login")
    public String login(@WebParam(name = "user") String user,
            @WebParam(name = "hash_pass") String hash_pass)
            throws AuthException, ConnectionException {
        String monitor_token = null;
        Database db = new Database();
        RLF_Cipher cipher = new RLF_DummyCipher();
        Connection conn = db.connect();
        if (conn == null) {
            RLF_Log.ProviderLog().severe("[LOGIN] Operación de login cancelada.");
            throw new ConnectionException();
        }

        try {

            // 1. Verificación del usuario.
            CallableStatement stmt = conn.prepareCall("{CALL loginMonitor(?)}");
            stmt.setString("client_hash", cipher.getHash(user + hash_pass));
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                // Fallo de autentificación.
                rs.close();
                stmt.close();
                db.disconnect(conn);
                throw new AuthException();
            }

            monitor_token = rs.getString("monitor");
            rs.close();
            stmt.close();
            db.disconnect(conn);

        } catch (SQLException sqle) {
            RLF_Log.ProviderLog().log(Level.SEVERE, "[LOGIN] Problema de comunicación "
                    + "con la base de datos: {0}", sqle.getMessage());
            db.disconnect(conn);
            throw new ConnectionException();
        }

        return monitor_token;
    }
    
    /**
     * Indica al proveedor que el monitor se desconecta. Sólo se elimina su token
     * de acceso.
     * 
     * @param monitor Token de acceso del monitor.
     * @return Verdadero.
     * @throws AuthException Fallo de autentificación.
     * @throws ConnectionException Fallo de conexión con los módulos de RLF.
     */
    @WebMethod(operationName = "logout")
    public Boolean logout(@WebParam(name = "monitor") String monitor)
            throws AuthException, ConnectionException {

        Database db = new Database();
        
        
        CallableStatement s = null;
        ResultSet rs = null;
        String host;
        String name;
        Connection conn = db.connect();
        if (conn == null) {
            RLF_Log.ProviderLog().severe("[LOGOUT] Operación de logout cancelada.");
            throw new ConnectionException();
        }

        // 1. Verificación del token.
        name = authMonitor(conn, monitor);
        if (name == null) {
            db.disconnect(conn);
            throw new AuthException();
        }

        try {


            // 2. Eliminación del token.
            s = conn.prepareCall("{CALL logoutMonitor(?)}");
            s.setString("client_name", name);
            s.execute();
            s.close();
            db.disconnect(conn);

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
     * @param monitor Token de acceso.
     * @return Array en formato Json con objetos de forma (id de la herramienta,
     *         estado). [{"id":"...", "name":"...", "status":"..."}, ...]
     * @throws AuthException Fallo de autentificación.
     * @throws ConnectionException Fallo de conexión con los módulos de RLF.
     */
    @WebMethod(operationName = "getStatus")
    public ArrayList<ToolInfo> getStatus(@WebParam(name = "monitor") String monitor)
            throws AuthException, ConnectionException {

        ArrayList<ToolInfo> tools = new ArrayList<ToolInfo>();
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
        name = authMonitor(conn, monitor);
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

                String id = new Integer(rs.getInt("id")).toString();
                String toolname = rs.getString("name");
                ToolStatus status;
                if (rs.getString("status").compareToIgnoreCase("ONLINE") == 0){
                    status = ToolStatus.ONLINE;
                } else if (rs.getString("status").compareToIgnoreCase("IN USE") == 0){
                    status = ToolStatus.INUSE;
                } else {
                    status = ToolStatus.OFFLINE;
                }
                
                tools.add(new ToolInfo(id, toolname, status));

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

        return tools;
    }
    
    /**
     * Autentifica a un monitor por su token de acceso.
     * @param conn Conexión abierta con la base de datos.
     * @param monitor Token de acceso proporcionado por el servicio web.
     * @return Nombre del usuario, null si hay un fallo de autentificación.
     */
    private String authMonitor(Connection conn, String monitor) {

        String name = null;

        try {

            // 1. Verificación del token.
            CallableStatement s = conn.prepareCall("{CALL verifymonitortoken(?)}");
            s.setString("client_monitor", monitor);
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
