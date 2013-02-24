/**
 * Salida de la acción.
 */
package org.rlf.lab.tool.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;

import org.rlf.lab.tool.Tool;
import org.rlf.log.RLF_Log;

/**
 * Gestiona la salida de una acción en ejecución. La salida puede estar mezclada
 * con la salida de error estándar. Obtiene los datos de un stream dado y los
 * escribe en el socket correspondiente.
 * 
 * @author Carlos A. Rodriguez Mecha
 * @version 0.1
 */
public class OutStream extends Thread {

	// Constantes:
	/** Tamaño del buffer de lectura y escritura. */
	public final static int BUFFER_SIZE = 4096;
	/** Tiempo de "muestreo" en milisegundos. */
	public final static int DELAY = 1000;

	// Atributos:
	/** Canal de escritura. */
	private ReadableByteChannel stream;
	/** Socket maestro de escucha. */
	private ServerSocketChannel master;
	/**
	 * Lista con las conexiones activas. Si no es una herramienta de datos, como
	 * máximo será una.
	 */
	private LinkedList<SocketChannel> connections;
	/** Herramienta de datos. */
	private boolean dataTool;
	/** Indica una petición de parada. */
	private boolean stopped;

	// Constructor:
	/**
	 * Constructor del hilo de salida del proceso.
	 * 
	 * @param master
	 *            Socket de escucha ya conectado. Puede ser null si la
	 *            herramienta sólo necesita liberar su buffer de salida y no va
	 *            a enviar nada a los clientes.
	 * @param out
	 *            Salida del programa.
	 * @param dataTool
	 *            Indica si la herramienta es de datos para permitir múltiples
	 *            conexiones.
	 */
	public OutStream(Tool t, ServerSocketChannel master, InputStream out,
			boolean dataTool) {
		super("OutStream " + t.getId());
		this.stream = Channels.newChannel(out);
		this.master = master;
		this.connections = new LinkedList<SocketChannel>();
		this.dataTool = dataTool;
		this.stopped = false;

	}

	// Métodos varios:
	/**
	 * Para el hilo de forma segura.
	 */
	public synchronized void stopStream() {
		this.stopped = true;
	}

	/**
	 * Limpia la salida de la acción. Esto ocurre cuando el socket de escucha es
	 * introducido como null porque no se aceptan conexiones con el cliente.
	 * 
	 * Se lleva a cabo porque algunos sistemas operativos bloquean el proceso
	 * hasta que se vacía por completo su salida y error estándar, por lo que
	 * hay que liberarlo cada cierto tiempo a pesar que no sea información útil.
	 */
	private void cleanStream() {

		ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

		while (true) {

			synchronized (this) {
				if (this.stopped) {
					try {
						this.stream.close();
					} catch (IOException e) {
					}
					break;
				}
			}

			try {
				buffer.clear();
				this.stream.read(buffer);
				Thread.sleep(DELAY);
			} catch (Exception e) {
				this.stopped = true;
			}

		}

	}

	/**
	 * Ejecución del hilo. Lee la salida del proceso y la redirige a los
	 * clientes.
	 */
	@Override
	public void run() {

		if (this.master == null) {
			cleanStream();
			return;
		}

		ServerSocketChannel ssc;
		SocketChannel s;
		Selector selector;
		Iterator<SelectionKey> it;
		ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
		int n = 0;

		try {
			selector = Selector.open();
			master.register(selector, SelectionKey.OP_ACCEPT);
		} catch (IOException io) {
			RLF_Log.LabLog().warning("El selector no ha podido ser abierto.");
			return;
		}

		while (true) {

			// 1. Parada del hilo.
			synchronized (this) {
				if (this.stopped) {

					for (SocketChannel socket : this.connections) {
						try {
							socket.close();
						} catch (IOException io) {
						}
					}

					try {
						this.master.close();
						selector.close();
						this.stream.close();
					} catch (IOException io) {
					}

					break;

				}
			}

			// 2. Lectura de la salida.
			try {
				if (!connections.isEmpty()) {
					buffer = ByteBuffer.allocate(BUFFER_SIZE);
					buffer.clear();
					n = this.stream.read(buffer);
				}
			} catch (IOException e) {
				this.stopped = true;
				continue;
			}

			// 3. Escucha de socket.
			try {
				if (selector.select(DELAY) <= 0)
					continue;
			} catch (IOException e) {
				RLF_Log.LabLog().warning(
						"El selector no ha podido ser utilizado.");
			}

			it = (selector.selectedKeys()).iterator();

			while (it.hasNext()) {
				SelectionKey key = (SelectionKey) it.next();

				// 3.1 Aceptación de nuevas conexiones.
				if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
					ssc = (ServerSocketChannel) key.channel();

					if (dataTool || (connections.size() == 0)) {
						try {
							s = ssc.accept();
							s.configureBlocking(false);
							s.register(selector, SelectionKey.OP_WRITE);

							this.connections.add(s);
						} catch (IOException io) {
						}

					}

					// 3.2 Escritura en el socket.
				} else if ((key.readyOps() & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE) {

					s = (SocketChannel) key.channel();
					try {
						if (n > 0) {
							buffer.rewind();
							s.write(buffer);
						}
					} catch (Exception e) {
						try {
							s.close();
						} catch (IOException e1) {
						}
						this.connections.remove(s);
					}

				}

				it.remove();
			}

		}

	}

}
