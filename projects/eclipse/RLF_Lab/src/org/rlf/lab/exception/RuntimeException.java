/**
 * Excepción de ejecución.
 */
package org.rlf.lab.exception;

/**
 * Excepción de la ejecución de una acción.
 * 
 * @author Carlos A. Rodriguez Mecha
 * @version 0.1
 */
public class RuntimeException extends Exception {

	// Constantes:
	/** Mensaje de la excepción. */
	public final static String EXCEPTION_MSG = "Problema al ejecutar la acción.";
	
	// Atributos:
	/** Serial Version. */
	private static final long serialVersionUID = 8348369699793896955L;
	
	// Constructor:
	/**
	 * Constructor.
	 */
	public RuntimeException() {
		super(EXCEPTION_MSG);
	}
	
}
