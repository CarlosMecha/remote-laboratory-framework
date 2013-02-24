/*
 * Excepción de fallo de autentificación.
 */
package org.rlf.provider.ws.exception;

/**
 * Representa un fallo con la autentificación del usuario.
 * 
 * @author Carlos A. Rodríguez Mecha
 * @version 0.1
 */
public class AuthException extends Exception {
    
    // Constantes:
    /** Mensaje del error. */
    public final static String ERR_MSG = "Error de autentificación";
    
    // Constructor:
    /**
     * Constructor de la excepción.
     */
    public AuthException(){
        super(ERR_MSG);
    }
    
}
