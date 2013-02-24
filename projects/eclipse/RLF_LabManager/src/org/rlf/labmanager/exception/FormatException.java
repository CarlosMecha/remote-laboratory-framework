package org.rlf.labmanager.exception;

/**
 * Excepción al registrar una herramienta con un formato inválido.
 * 
 * @author Carlos A. Rodriguez Mecha
 * @version 0.1
 */
public class FormatException extends LabManagerException {
	
	// Constantes:
	/** Mensaje de la excepción. */
	public final static String EXCEPTION_MSG = "La herramienta tiene un formato inválido.";
	
	// Atributos:
	/** Serial Version. */
	private static final long serialVersionUID = 2566434233976375930L;
	
	
	// Constructor:
	/**
	 * Constructor.
	 */
	public FormatException() {
		super(EXCEPTION_MSG);
	}

}
