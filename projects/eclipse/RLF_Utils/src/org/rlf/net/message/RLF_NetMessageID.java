/**
 * Todos los tipos de operaciones soportadas por el Lab, el Provider y sus correspondientes gestores.
 */
package org.rlf.net.message;

/**
 * Identificación de todos los tipos de mensajes posibles en RLF.
 * 
 * @author Carlos A. Rodriguez Mecha
 * @version 0.1
 */
public enum RLF_NetMessageID {

	/** No válida. */
	NULL(-1),
	/** Sí. */
	OK(0),
	/** No. */
	NO(1),
	/** Fallo de autentificación. */
	AUTHFAIL(2),
	/** Fallo de formato. */
	FORMATERROR(3),
	/** Clave e identificador asignada a la herramienta. */
	TOOLKEY(4),
	/** Obtención del estado del laboratorio y si está activo, de sus herramientas. */
	LABSTATUS(10),
	/** Activación del laboratorio. */
	ARMLAB(11),
	/** Desactivación del laboratorio. */
	DISARMLAB(12),
	/** Registro de una herramienta. */
	REGISTRYTOOL(13),
	/** Envío de una herramienta. */
	TOOL(14),
	/** Eliminación de una herramienta. */
	DROPTOOL(15),
	/** Notificación de conexión de un laboratorio. */
	STARTLAB(16),
	/** Notificación de desconexión de un laboratorio. */
	STOPLAB(17),
	/** Parada de emergencia. */
	EMERGENCYSTOP(18),
	/** Parada segura o desconexión del laboratorio. */
	SAFESTOP(19),
	/** Envío del token de un nuevo usuario. */
	TOKEN(20),
	/** Desconexión voluntaria del usuario. */
	LOGOUT(21),
	/** Autentificación de un administrador. */
	AUTHADMIN(22),
	/** Tiempo de usuario acabado. */
	TIMEOUT(23),
	/** Comienza la ejecución de una acción. */
	EXEC(24),
	/** Ejecución terminada correctamente o petición de finalización. */
	EXEC_FINISH(25),
	/** Ejecución terminada con errores. */
	EXEC_ERROR(26),
	/** Autentificación de un socket. */
	AUTH(27);
	
	// Atributos:
	/** Número de operación. */
	private int id;
	
	// Constructor:
	/** Constructor del identificador. */
	private RLF_NetMessageID(int id){
		this.id = id;
	}
	
	// Métodos varios.
	/**
	 * Representación textual del elemento.
	 * @return Número de operación.
	 */
	@Override
	public String toString(){
		return (new Integer(this.id)).toString();
	}
	
	/**
	 * Obtiene el elemento por medio de su identificador.
	 * @param id Identificador de la operación.
	 * @return Operación.
	 */
	public static RLF_NetMessageID valueOf(int id){
		
		switch (id){
		case -1: return NULL;
		case 0: return OK;
		case 1: return NO;
		case 2: return AUTHFAIL;
		case 3: return FORMATERROR;
		case 4: return TOOLKEY;
		case 10: return LABSTATUS;
		case 11: return ARMLAB;
		case 12: return DISARMLAB;
		case 13: return REGISTRYTOOL;
		case 14: return TOOL;
		case 15: return DROPTOOL;
		case 16: return STARTLAB;
		case 17: return STOPLAB;
		case 18: return EMERGENCYSTOP;
		case 19: return SAFESTOP;
		case 20: return TOKEN;
		case 21: return LOGOUT;
		case 22: return AUTHADMIN;
		case 23: return TIMEOUT;
		case 24: return EXEC;
		case 25: return EXEC_FINISH;
		case 26: return EXEC_ERROR;
		case 27: return AUTH;
		default: return NULL;
		
		}
		
	}
	
}
