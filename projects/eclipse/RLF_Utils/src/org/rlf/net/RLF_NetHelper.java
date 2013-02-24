/**
 * Envía y recibe datos en el formato especificado.
 */
package org.rlf.net;

import java.nio.*;
import java.nio.channels.SocketChannel;
import org.rlf.net.message.RLF_NetMessage;


/**
 * Gestor de envío y recepción de mensajes de operaciones o peticiones.
 * 
 * @author Carlos A. Rodriguez Mecha
 * @version 0.1
 */
public class RLF_NetHelper {

	// Constantes:
	/** Tamaño del bloque de envío. */
	public final static int BLOCK_SIZE = 2048;

	// Constructor:
	/**
	 * Constructor del asistente.
	 */
	public RLF_NetHelper() {
	}

	// Métodos de red:
	/**
	 * Envía un mensaje por el socket introducido.
	 * 
	 * @param msg
	 *            Mensaje a enviar.
	 * @param socket
	 *            Socket de envío.
	 * @return Verdadero si ha podido ser enviado. Falso en caso contrario.
	 */
	public boolean sendMessage(RLF_NetMessage msg, SocketChannel socket) {

		String pack = msg.pack();
		String netpack = pack.length() + "|" + pack;
		ByteBuffer buffer = ByteBuffer.allocate(BLOCK_SIZE);

		int i = 0, f = 0;

		while (i < pack.length()) {

			// 1. Relleno del buffer.
			f = i + BLOCK_SIZE;
			if (f > netpack.length())
				f = netpack.length();
			buffer.clear();
			buffer.put(netpack.substring(i, f).getBytes(), 0, f - i);
			buffer.flip();

			try {

				// 2. Envío.
				socket.write(buffer);

			} catch (Exception e) {
				return false;
			}

			i = f;

		}

		return true;
	}

	/**
	 * Obtiene el mensaje recibido por el socket.
	 * 
	 * @param socket
	 *            Socket de envío.
	 * @return Mensaje recibido. Null si ha habido fallos.
	 */
	public RLF_NetMessage reciveMessage(SocketChannel socket) {

		String pack = new String();
		RLF_NetMessage msg = new RLF_NetMessage();
		ByteBuffer buffer = ByteBuffer.allocate(BLOCK_SIZE);
		int length = -1, n = 0, read;
		boolean first = true;

		try {

			// 1. Creación del paquete.
			while (n != length) {

				buffer.clear();
				read = socket.read(buffer);
				n += read;
				buffer.flip();
				byte[] b = new byte[read];
				buffer.get(b, 0, read);
				pack += new String(b);

				// 1.1 Lectura de la longitud del mensaje.
				if (first) {
					length = Integer.parseInt(pack.substring(0,
							pack.indexOf('|')));
					pack = pack.substring(pack.indexOf('|') + 1);
					n = pack.length();
					first = false;
				}

			}

			// 2. Descodificación.
			if (!msg.unpack(pack))
				return null;

		} catch (Exception e) {
			return null;
		}

		return msg;
	}

}
