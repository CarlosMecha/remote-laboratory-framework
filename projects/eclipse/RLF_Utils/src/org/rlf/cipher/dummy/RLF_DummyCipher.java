/**
 * Implementación de prueba.
 */
package org.rlf.cipher.dummy;

import java.security.MessageDigest;

import org.apache.commons.codec.binary.Base64;
import org.rlf.cipher.RLF_Cipher;

/**
 * Codifica en Base64. Implementación de prueba.
 * 
 * @author Carlos A. Rodriguez Mecha
 * @version 0.1
 */
public class RLF_DummyCipher implements RLF_Cipher {

	/**
	 * Codifica los datos con el algoritmo Base64.
	 * 
	 * @param bytes
	 *            Array de datos.
	 * @return Bytes cifrados.
	 * @see org.rlf.cipher.RLF_Cipher#encode(byte[])
	 */
	@Override
	public byte[] encode(byte[] bytes) {

		return Base64.encodeBase64(bytes);

	}

	/**
	 * Descodifica los datos con el algoritmo Base64.
	 * 
	 * @param bytes
	 *            Array de datos cifrados.
	 * @return Bytes descifrados.
	 * @see org.rlf.cipher.RLF_Cipher#decode(byte[])
	 */
	@Override
	public byte[] decode(byte[] bytes) {

		return Base64.decodeBase64(bytes);

	}

	/**
	 * Realiza una codificación hash segura mediante el algoritmo SHA-1. Si no
	 * está disponible en la plataforma, se utilizará el algoritmo de Java por
	 * defecto de Hash.
	 * 
	 * @param text
	 *            Texto a codificar.
	 * @return Texto cifrado.
	 * @see org.rlf.cipher.RLF_Cipher#getHash(java.lang.String)
	 */
	@Override
	public String getHash(String text) {
		StringBuilder hash = null;
		try {
			hash = new StringBuilder();
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			digest.reset();
			for (byte b : digest.digest(text.getBytes("UTF-8"))) {
				String n = Integer.toHexString(0xFF & b);
				if (n.length() == 1)
					hash.append('0');
				hash.append(Integer.toHexString(0xFF & b));
			}
		} catch (Exception e) {
			return new Integer(text.hashCode()).toString();
		}
		return hash.toString();
	}

}
