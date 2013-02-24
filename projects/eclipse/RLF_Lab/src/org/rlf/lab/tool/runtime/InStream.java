/**
 * Entrada de la acción.
 */
package org.rlf.lab.tool.runtime;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import org.rlf.log.RLF_Log;

/**
 * Gestiona la entrada de una acción en ejecución. Obtiene los datos de un
 * socket dado y los escribe en el stream correspondiente.
 * 
 * @author Carlos A. Rodriguez Mecha
 * @version 0.1
 */
public class InStream extends Thread {

	// Constantes:
	/** Tamaño del buffer de lectura y escritura. */
	public final static int BUFFER_SIZE = 4096;
	/** Tiempo de "muestreo" en milisegundos. */
	public final static int DELAY = 1000;

	// Atributos:
	/** Canal de escritura. */
	private BufferedWriter stream;
	/** Socket maestro de escucha. */
	private ServerSocketChannel master;
	/** Socket conectado al cliente. */
	private SocketChannel client;
	/** Indica una petición de parada. */
	private boolean stopped;

	// Constructor:
	/**
	 * Constructor del hilo de entrada del proceso.
	 * 
	 * @param master
	 *            Socket de escucha ya conectado.
	 * @param in
	 *            Entrada del programa.
	 */
	public InStream(ServerSocketChannel master, OutputStream in) {
		super("InStream");
		this.stream = new BufferedWriter(new OutputStreamWriter(in));
		this.master = master;
		this.client = null;
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
	 * Ejecución del hilo. Lee los datos enviados por el cliente para
	 * introducirlos en la entrada estándar del proceso.
	 */
	@Override
	public void run() {

		ServerSocketChannel ssc;
		SocketChannel s;
		Selector selector;
		int n = 0;
		Iterator<SelectionKey> it;
		ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

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

					try {

						this.master.close();
						this.stream.close();
						selector.close();
						if (this.client != null)
							this.client.close();

					} catch (IOException io) {

					}

					break;

				}
			}

			// 2. Escucha de sockets.
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

				// 2.1 Aceptación de conexión.
				if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
					ssc = (ServerSocketChannel) key.channel();
					if (this.client == null) {
						try {
							this.client = ssc.accept();
							this.client.configureBlocking(false);
							this.client
									.register(selector, SelectionKey.OP_READ);
						} catch (IOException e) {
							this.client = null;
						}

					}

					// 2.2 Escritura en la entrada.
				} else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {

					s = (SocketChannel) key.channel();
					buffer.clear();

					// 2.2.1 Lectura.
					try {
						n = s.read(buffer);
					} catch (Exception e) {
						// Canal cerrado.
						try {
							s.close();
						} catch (IOException e1) {
						}
						key.cancel();
						this.client = null;
					}

					buffer.rewind();

					// 2.2.2 Escritura.
					if (n > 0) {
						byte[] b = new byte[n];
                        buffer.get(b, 0, n);
                        String in = new String(b).trim() + System.getProperty("line.separator");
						try {
							this.stream.write(in);
							this.stream.flush();
						} catch (IOException e) {
						}
					}

				}

				it.remove();

			}

		}

	}

}
