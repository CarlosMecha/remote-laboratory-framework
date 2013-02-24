package org.rlf.labmanager.exception;

/**
 * Excepción al autentificar el LabManager o el administrador.
 * 
 * @author Carlos A. Rodriguez Mecha
 * @version 0.1
 */
public class AuthException extends LabManagerException {

	// Constantes:
	/** Mensaje de la excepción. */
	public final static String EXCEPTION_MSG = "Fallo al autentificar el LabManager o el administrador.";
	
	// Atributos:
	/** Serial Version. */
	private static final long serialVersionUID = -3901251507789163623L;

	
	// Constructor:
	/**
	 * Constructor. Representa un fallo de autentificación.
	 */
	public AuthException() {
		super(EXCEPTION_MSG);
	}

}
