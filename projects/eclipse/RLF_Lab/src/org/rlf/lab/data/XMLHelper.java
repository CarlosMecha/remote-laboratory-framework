/**
 * Clase para la utilización de XMLs.
 */
package org.rlf.lab.data;

import java.io.File;
import java.io.IOException;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.rlf.log.RLF_Log;

/**
 * Asistente para la validación y lectura de XMLs de la definición de las
 * herramientas.
 * 
 * @author Carlos A. Rodriguez Mecha
 * @version 0.1
 */
public class XMLHelper {

	// Constructor:
	/**
	 * Constructor del asistente.
	 */
	public XMLHelper() {
	}

	// Métodos varios:
	/**
	 * Obtiene el documento XML validado por su DTD. Este DTD debe estar bien
	 * localizado en la ruta incluida en el propio fichero XML.
	 * 
	 * @param xml
	 *            Fichero XML.
	 * @return Verdadero si ha sido validado. Null si no es válido o no ha
	 *         podido abrir el fichero.
	 */
	public Document read(File xml) {
		try {

			SAXBuilder builder = new SAXBuilder(true);
			Document jdoc = builder.build(xml);
			return jdoc;

		} catch (JDOMException e) {
			RLF_Log.Log().warning("Documento XML inválido de la herramienta.");
			return null;
		} catch (IOException e) {
			RLF_Log.Log().warning("No se ha podido obtener el fichero XML de la herramienta.");
			return null;
		}
	}

}
