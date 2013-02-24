/*
 * Excepción de formato de mensaje.
 */
package org.rlf.provider.ws.exception;

/**
 * Se produce cuando el cliente envía un mensaje con un formato inválido.
 * 
 * @author Carlos A. Rodríguez Mecha
 * @version 0.1
 */
public class FormatException extends Exception {
   
    // Constantes:
    /** Mensaje del error. */
    public final static String ERR_MSG = "El mensaje enviado tiene un formato incorrecto.";
    
    // Constructor:
    /**
     * Constructor de la excepción.
     */
    public FormatException(){
        super(ERR_MSG);
    }
    
}
