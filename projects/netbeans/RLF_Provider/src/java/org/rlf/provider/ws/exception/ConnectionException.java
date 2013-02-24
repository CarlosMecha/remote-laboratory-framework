/*
 * Problema de conexión con la base de datos o con los laboratorios.
 */
package org.rlf.provider.ws.exception;

/**
 * Excepción de comunicación con los diferentes módulos de RLF.
 * 
 * @author Carlos A. Rodríguez Mecha
 * @version 0.1
 */
public class ConnectionException extends Exception {
   
    // Constantes:
    /** Mensaje del error. */
    public final static String ERR_MSG = "Error de comunicación con los módulos.";
    
    // Constructor:
    /**
     * Constructor de la excepción.
     */
    public ConnectionException(){
        super(ERR_MSG);
    }
    
}
