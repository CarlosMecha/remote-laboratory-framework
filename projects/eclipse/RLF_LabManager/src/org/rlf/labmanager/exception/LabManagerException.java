/**
 * Excepción principal de la librería.
 */
package org.rlf.labmanager.exception;

/**
 * Excepción genérica.
 * @author Carlos A. Rodriguez Mecha
 * @version 0.1
 */
public abstract class LabManagerException extends Exception {

	// Atributos:
	/** Serial Version. */
	private static final long serialVersionUID = 391674670763726178L;

	// Constructores:
	/**
	 * Constructor por defecto. Corresponde a un error desconocido.
	 */
	public LabManagerException() {
		super("Error desconocido.");
	}

	/**
	 * Constructor.
	 * @param arg0 Mensaje de la excepción.
	 */
	public LabManagerException(String arg0) {
		super(arg0);
	}
	
}
