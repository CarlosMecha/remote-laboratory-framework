/*
 * Excepción.
 */
package org.rlf.client.provider;

/**
 * Excepción de conexión con el proveedor.
 * @author Carlos A. Rodríguez Mecha
 * @version 0.1
 */
public class ProviderException extends Exception {
    
    // Constantes:
    /** Mensaje. */
    public final static String MSG = "Fallo de conexión con el proveedor.";
    
    
    // Constructor:
    /**
     * Constructor de la excepción.
     */
    public ProviderException(){
        super(MSG);
    }
}
