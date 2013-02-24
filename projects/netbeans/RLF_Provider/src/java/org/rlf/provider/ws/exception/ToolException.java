/*
 * Error generado por una herramienta.
 */
package org.rlf.provider.ws.exception;

/**
 * Excepción lanzada por una herramienta o su laboratorio.
 * 
 * @author Carlos A. Rodríguez Mecha
 * @version 0.1
 */
public class ToolException extends Exception {
   
    // Constantes:
    /** Mensaje del error. */
    public final static String ERR_MSG = "La herramienta no se puede usar.";
    
    // Constructor:
    /**
     * Constructor de la excepción.
     */
    public ToolException(){
        super(ERR_MSG);
    }
    
}
