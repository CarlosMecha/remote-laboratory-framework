/**
 * Acción.
 */
package org.rlf.lab.tool.runtime;

import java.io.*;
import java.nio.channels.*;
import java.util.Timer;
import java.util.TimerTask;

import org.rlf.cipher.RLF_Cipher;
import org.rlf.cipher.dummy.RLF_DummyCipher;
import org.rlf.lab.tool.Tool;
import org.rlf.log.RLF_Log;

/**
 * Hilo que se encarga de gestionar una ejecución de una acción concreta. Cada
 * acción en ejecución tiene unos puertos asignados con la salida y entrada del
 * proceso conectadas a sockets para que los clientes puedan utilizarlas. Estas
 * acciones pueden ser paradas por una emergencia o por sobrepasar el tiempo
 * máximo asignado en la descripción de la herramienta. También estas acciones
 * pueden ser "resets", es decir, devuelven las herramientas a su estado
 * original.
 * 
 * @author Carlos A. Rodriguez Mecha
 * @version 0.1
 */
public class Action extends Thread {

	// Constantes:
	/** Tiempo de "muestreo" de lecturas en milisegundos. */
	public final static int DELAY = 1000;
	/** Tiempo máximo de ejecución de una acción reseteadora en segundos. */
	public final static int RESET_TIMEOUT = 180000;

	// Atributos:
	/** Herramienta. */
	private Tool tool;
	/** Tarea que se ejecuta cuando pasa el tiempo máximo de ejecución. */
	private TimerTask taskTimeout;
	/** Comando que implica la acción. */
	private String command;
	/** Socket de entrada. */
	private ServerSocketChannel socketIn;
	/** Socket de salida. */
	private ServerSocketChannel socketOut;
	/** Tiempo en segundos máximo de ejecución del comando. */
	private int timeout;
	/** Programa en ejecución. */
	private Process process;
	/** Ticket de la acción. Identificador único. */
	private String ticket;
	/** Stream de entrada del proceso. */
	private InStream in;
	/** Stream de salida del proceso. */
	private OutStream out;
	/** Timer de la acción. */
	private Timer timer;
	/** La acción ha sido interrumpida. */
	private boolean stopped;

	/** Contador de tickets. */
	private static int ntickets = 0;

	// Constructor:
	/**
	 * Constructor para una acción que resetea la herramienta.
	 * 
	 * @param tool
	 *            Herramienta.
	 * 
	 * @param commandReset
	 *            Comando de reseteo del hardware.
	 */
	public Action(Tool tool, String commandReset) {
		super(commandReset + ntickets);
		this.tool = tool;
		this.command = tool.getPath() + File.separator + commandReset;
		this.socketIn = null;
		this.socketOut = null;
		this.timeout = RESET_TIMEOUT;
		this.taskTimeout = new TimeoutTask(this);
		this.process = null;
		this.in = null;
		this.out = null;
		this.stopped = false;
		
		RLF_Cipher cipher = new RLF_DummyCipher();
		java.sql.Timestamp current = new java.sql.Timestamp(java.util.Calendar.getInstance().getTime().getTime());
		this.ticket = cipher.getHash(current.toString() + ntickets++);
		
	}

	/**
	 * Constructor de una acción.
	 * 
	 * @param tool
	 *            Herramienta.
	 * @param command
	 *            Comando.
	 * @param socketIn
	 *            Socket de entrada del programa. Null si no es necesario.
	 * @param socketOut
	 *            Socket de salida del programa. Null si no es necesario.
	 * @param timeout
	 *            Tiempo en minutos máximo de ejecución de la acción.
	 */
	public Action(Tool tool, String command, ServerSocketChannel socketIn,
			ServerSocketChannel socketOut, int timeout) {
		super(command + ntickets);
		this.tool = tool;
		this.command = tool.getPath() + File.separator + command;
		this.socketIn = socketIn;
		this.socketOut = socketOut;
		this.timeout = timeout * 60;
		this.taskTimeout = new TimeoutTask(this);
		this.process = null;
		this.in = null;
		this.out = null;
		this.stopped = false;
		
		RLF_Cipher cipher = new RLF_DummyCipher();
		java.sql.Timestamp current = new java.sql.Timestamp(java.util.Calendar.getInstance().getTime().getTime());
		this.ticket = cipher.getHash(current.toString() + ntickets++);
		
	}

	// Métodos getters y setters:
	/**
	 * Obtiene el comando asociado a la acción.
	 * 
	 * @return El comando asignado.
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * Obtiene el identificador de la herramienta asociada.
	 * 
	 * @return Identificador.
	 */
	public int getToolId() {
		return tool.getId();
	}

	/**
	 * Obtiene el ticket asignado a la acción.
	 * 
	 * @return Ticket.
	 */
	public String getTicket() {
		return ticket;
	}

	// Métodos varios:
	/**
	 * Realiza la parada del comando en ejecución actual. Puede ser por exceder
	 * el tiempo máximo de ejecución o por una parada de emergencia. Si se
	 * utiliza es necesario hacer después un reseteo de la herramienta.
	 */
	public synchronized void stopAction() {

		this.stopped = true;
		
		if (!this.isAlive())
			return;
		
		if (process != null) {
			process.destroy();
		}

	}

	/**
	 * Ejecución de la acción. Si es una acción normal, utiliza sockets para que
	 * los clientes se conecten a la entrada y salidas del propio comando. Si es
	 * una herramienta de datos, permite que varios clientes se conecten a las
	 * salidas, pero no a la entrada.
	 */
	@Override
	public void run() {

		// 1. Directorio de ejecución.
		File directory = new File(tool.getPath() + File.separator);
		timer = new Timer();

		// 2. Ejecución del proceso.
		tool.setStatus(Tool.ToolStatus.RUNNING);
		ProcessBuilder pb = new ProcessBuilder(command.split(" "));
		pb.redirectErrorStream(true);
		pb.directory(directory);
		timer.scheduleAtFixedRate(this.taskTimeout, this.timeout * 1000, this.timeout * 1000);

		try {
			tool.setStatus(Tool.ToolStatus.RUNNING);
			Thread.sleep(DELAY * 4);
			process = pb.start();
		} catch (Exception e) {
			RLF_Log.LabLog().severe(
					"No se ha podido ejecutar la "
							+ "herramienta por fallo en ejecución. ("
							+ this.command + ")");
			this.timer.cancel();
			return;
		}

		// 3. Redirección.
		synchronized (this) {
			if (socketIn != null) {
				this.in = new InStream(socketIn, process.getOutputStream());
				this.in.start();
			}
			this.out = new OutStream(tool, socketOut, process.getInputStream(),
					this.tool.isDataTool());
			this.out.start();
		}

		// 4. Se espera hasta la terminación de la acción.
		try {
			this.process.waitFor();
			if (this.in != null) {
				in.stopStream();
			}
			this.out.stopStream();
			if (!this.stopped) 
				tool.setStatus(Tool.ToolStatus.ACTIVE);
			else tool.setStatus(Tool.ToolStatus.OFF);
		} catch (InterruptedException ie) {
			tool.setStatus(Tool.ToolStatus.OFF);
			this.timer.cancel();
			this.timer.purge();
		}

		// 5. Finalización.
		try {
			if (this.in != null)
				this.in.join(DELAY * 2);
			this.out.join(DELAY * 2);
			if (this.out.isAlive()) this.out.interrupt();
			this.timer.cancel();
			this.timer.purge();
		} catch (InterruptedException e) {
			this.timer.cancel();
			this.timer.purge();
		}

	}

	/**
	 * Clase de la tarea específica de tiempo máximo excedido de la acción.
	 */
	private class TimeoutTask extends TimerTask {

		// Atributos:
		/** Acción. */
		private Action action;

		// Constructor:
		/**
		 * Constructor de la tarea.
		 * 
		 * @param action
		 *            Acción concreta.
		 */
		public TimeoutTask(Action action) {
			this.action = action;
		}

		// Métodos varios:
		/**
		 * Tarea a ejecutar.
		 */
		@Override
		public void run() {
			this.action.stopAction();
			timer.cancel();
			timer.purge();
		}
	}

}
