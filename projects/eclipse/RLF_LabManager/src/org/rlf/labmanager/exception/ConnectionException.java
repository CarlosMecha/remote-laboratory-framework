package org.rlf.labmanager.exception;

/**
 * Excepción al intentar conectar.
 * @author Carlos A. Rodriguez Mecha
 * @version 0.1
 */
public class ConnectionException extends LabManagerException {

	// Constantes:
	/** Mensaje de la excepción. */
	public final static String EXCEPTION_MSG = "No se ha podido conectar con el laboratorio.";
	
	// Atributos:
	/** Serial Version. */
	private static final long serialVersionUID = -6921785835339087495L;
	
	
	// Constructor:
	/**
	 * Constructor.
	 */
	public ConnectionException() {
		super(EXCEPTION_MSG);
	}

}
