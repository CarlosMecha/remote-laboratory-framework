/**
 * Excepción DB.
 */
package org.rlf.lab.exception;

/**
 * Excepción de la base de datos de una herramienta o del propio laboratorio.
 * 
 * @author Carlos A. Rodriguez Mecha
 * @version 0.1
 */
public class DBException extends Exception {

	// Constantes:
	/** Mensaje de la excepción. */
	public final static String EXCEPTION_MSG = "Problema con la conexión, escritura o lectura de la base de datos.";

	// Atributos:
	/** Serial Version. */
	private static final long serialVersionUID = 4831674660582516946L;

	// Constructor:
	/**
	 * Constructor. Representa a una excepción por fallo de conexión, escritura
	 * o lectura de alguna de las bases de datos.
	 */
	public DBException() {
		super(EXCEPTION_MSG);
	}

}
