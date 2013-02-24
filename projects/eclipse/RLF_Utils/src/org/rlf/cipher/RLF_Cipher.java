/**
 * Interfaz base.
 */
package org.rlf.cipher;

/**
 * Interfaz para los cifradores RLF.
 * 
 * @author Carlos A. Rodriguez Mecha
 * @version 0.1
 */
public interface RLF_Cipher {

	/**
	 * Cifra un conjunto de bytes dependiendo de la implementación.
	 * 
	 * @param bytes
	 *            Array de datos.
	 * @return Bytes cifrados.
	 */
	public byte[] encode(byte[] bytes);

	/**
	 * Descifra el conjunto de bytes dependiendo de la implementación.
	 * 
	 * @param bytes
	 *            Array de datos cifrados.
	 * @return Bytes descifrados.
	 */
	public byte[] decode(byte[] bytes);

	/**
	 * Genera un hash de un texto.
	 * 
	 * @param text
	 *            Texto.
	 * @return Hash.
	 */
	public String getHash(String text);

}
