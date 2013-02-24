/*
 * Asistente para la base de datos.
 */
package org.rlf.provider.ws.data;

import java.sql.Connection;
import java.sql.DriverManager;
import org.rlf.log.RLF_Log;

/**
 * Asistente para la conexión con la base de datos del proveedor. Utiliza el
 * driver de MySQL.
 * 
 * @author Carlos A. Rodríguez Mecha
 * @version 0.1
 */
public class Database {

    // Constantes:
    /** Localización de la base de datos del proveedor. */
    public final static String DATABASE = "jdbc:mysql://localhost:3306/rlf";
    /** Usuario de acceso a la base de datos. */
    private static String USER = "provider";
    /** Contraseña del usuario. */
    private static String PASS = "rlfpass";

    // Constructor:
    /**
     * Constructor del asistente.
     */
    public Database() {
    }

    /**
     * Obtiene la conexión con la base de datos del proveedor.
     * @return Conexión. Null si ha habido algún fallo.
     */
    public Connection connect() {

        Connection connection = null;
        
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(DATABASE, USER, PASS);
        } catch (Exception e) {
            RLF_Log.ProviderLog().severe("[EXCEPTION] No se puede establecer conexión con la base de datos.");
            return null;
        }

        return connection;

    }

    /**
     * Desconecta la base de datos.
     * @param conn Conexión.
     */
    public void disconnect(Connection conn) {

        try {
            conn.close();
        } catch (Exception e) {
            RLF_Log.ProviderLog().severe("[EXCEPTION] No se puede desconectar de la base de datos.");
        }

    }
}
