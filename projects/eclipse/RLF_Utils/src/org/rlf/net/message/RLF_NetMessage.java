/**
 * Mensajes de red.
 */
package org.rlf.net.message;

import java.util.HashMap;
import java.util.Map.Entry;

import org.rlf.cipher.*;
import org.rlf.cipher.dummy.*;
import org.rlf.log.*;

import com.google.gson.*;

/**
 * Contenido a enviar o recibir para las peticiones y operaciones.
 * 
 * @author Carlos A. Rodriguez Mecha
 * @version 0.1
 */
public class RLF_NetMessage {

	// Atributos:
	/** Identificación de la operación. */
	private RLF_NetMessageID id;
	/** Lista de atributos descodificados. */
	private HashMap<String, String> attributes;

	// Constructores:
	/**
	 * Constructor por defecto.
	 */
	public RLF_NetMessage() {

		this.id = RLF_NetMessageID.NULL;
		this.attributes = new HashMap<String, String>();
	}

	/**
	 * Constructor del mensaje ya creado.
	 * 
	 * @param id
	 *            Identificador del mensaje.
	 * @param attributes
	 *            Lista de los atributos (textuales).
	 */
	public RLF_NetMessage(RLF_NetMessageID id,
			HashMap<String, String> attributes) {

		this.id = id;
		this.attributes = attributes;
	}

	// Métodos getters y setters:
	/**
	 * Obtiene la operación del mensaje.
	 * 
	 * @return Identificador del mensaje.
	 */
	public RLF_NetMessageID getId() {
		return this.id;
	}

	/**
	 * Obtiene los atributos de forma <clave, valor>.
	 * 
	 * @return Lista de atributos.
	 */
	public HashMap<String, String> getAttributes() {
		return this.attributes;
	}

	/**
	 * Modifica la operación del mensaje.
	 * 
	 * @param id
	 *            Nuevo identificador.
	 */
	public void setId(RLF_NetMessageID id) {
		this.id = id;
	}

	/**
	 * Modifica los atributos del mensaje.
	 * 
	 * @param attributes
	 *            Nueva lista de atributos.
	 */
	public void setAttributes(HashMap<String, String> attributes) {
		this.attributes = attributes;
	}

	// Métodos de red:
	/**
	 * Empaqueta un mensaje para su envío. El formato es el identificador de la
	 * operación y los atributos codificados.
	 * 
	 * @return Contenido del paquete.
	 */
	public String pack() {

		String encode_attributes;
		RLF_Cipher cipher = new RLF_DummyCipher();
		JsonObject object = new JsonObject();
		JsonObject obj_attributes = new JsonObject();

		// 1. Se incluyen los atributos.
		for (Entry<String, String> e : this.attributes.entrySet()) {
			obj_attributes.addProperty(e.getKey(), e.getValue());
		}

		// 2. Se codifican los atributos.
		encode_attributes = new String(cipher.encode((new Gson()
				.toJson(obj_attributes)).getBytes()));

		// 3. Se añade el atributo.
		object.addProperty(this.id.toString(), encode_attributes);

		return new Gson().toJson(object);

	}

	/**
	 * Desempaqueta un mensaje.
	 * 
	 * @param content Contenido
	 *            del paquete.
	 * @return Verdadero si se ha podido desempaquetar, en otro caso falso.
	 */
	public boolean unpack(String content) {

		JsonParser parser = new JsonParser();
		RLF_Cipher cipher = new RLF_DummyCipher();

		try {
			JsonElement element = parser.parse(content);
			if (!element.isJsonObject())
				return false;

			// 1. Se obtiene el identificador.
			JsonObject object = element.getAsJsonObject();
			String id = object.entrySet().iterator().next().getKey();
			try {
				this.id = RLF_NetMessageID.valueOf(Integer.parseInt(id));
			} catch (NumberFormatException ex) {
				throw new Exception();
			}

			// 2. Se descodifican los atributos.
			JsonObject obj_attributes = parser.parse(
					new String(cipher.decode(object.get(id).getAsString()
							.getBytes()))).getAsJsonObject();
			for (Entry<String, JsonElement> e : obj_attributes.entrySet()) {
				this.attributes.put(e.getKey(), e.getValue().getAsString());
			}

		} catch (Exception e) {
			RLF_Log.Log().warning(
					"[EXCEPTION] El mensaje tiene un formato desconocido: "
							+ e.getLocalizedMessage());
			return false;
		}

		return true;

	}
}
