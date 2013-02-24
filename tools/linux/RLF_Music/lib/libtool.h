/**
 * Remote Laboratory Framework
 *
 * Definición de la librería de conexión con el laboratorio. Se utiliza
 * cuando una acción está en ejecución y se desean obtener los parámetros
 * introducidos por el usuario, así como los atributos y las constantes.
 * También puede lanzar excepciones de ejecución y cambiar su estado
 * de finalización.
 * 
 * @author Carlos A. Rodríguez Mecha
 * @version 0.1
 */

// Definiciones:
#define RLF_SUCCESS 0
#define RLF_ERR_MEM -1
#define RLF_ERR_FILE -2
#define RLF_ERR_TOOL -3
#define RLF_ERR_CONN -4
#define RLF_ERR_KEY -5
#define RLF_ERR_DATA -6

// Tipos:
/** Tipo de datos. */
typedef enum datatype_t { string_type = 0, encode_type = 1, int_type = 2, long_type = 3, double_type = 4, boolean_type = 5 } Dtype;
/** Clase a la que pertenece el dato. */
typedef enum classtype_t { attribute_type = 0, constant_type = 1, parameter_type = 2 } Ctype;

// Estructuras:
/** Estructura principal de datos. */
typedef struct __rlf_data {
	/** Nombre del dato. */
	char * name;
	/** Clase del dato. */
	Ctype ctype;
	/** Tipo de dato. */
	Dtype dtype;
	/** Valor del dato. */
	char * value;
	/** Valor por defecto. (Sólo para parámetros de entrada.) */
	char * dfl;
	/** Valor máximo. (Sólo para parámetros de entrada.) */
	char * max;
	/** Valor mínimo. (Sólo para parámetros de entrada.) */
	char * min;
	/** Indica si el dato ha sido modificado desde la última ejecución. */
	int modified;
} RLF_Data;

// Funciones para los datos:
/**
 * Crea una nueva estructura vacía.
 * @return La estructura creada. Null si ha habido fallo de memoria.
 */
RLF_Data * RLF_Create();
/**
 * Destruye la estructura introducida.
 * @param data Estructura a destruir.
 */
void RLF_Destroy(RLF_Data * data);

// Funciones de comunicación:
/**
 * Conecta la herramienta con el laboratorio.
 * @param key Clave asignada a la herramienta.
 * @return Distinto de RLF_SUCCESS si no se ha podido conectar:
 * - RLF_ERR_CONN: Ya estaba conectado.
 * - RLF_ERR_FILE: La herramienta no ha sido registrada.
 * - RLF_ERR_TOOL: La herramienta no ha sido registrada.
 * - RLF_ERR_KEY: La clave introducida no es correcta.
 */
int RLF_Init(char * key);
/**
 * Desconecta la herramienta en ejecución del laboratorio.
 * @param status Valor de la salida de la acción.
 * @param desc Descripción de la salida de la acción.
 * @return Distinto de RLF_SUCCESS si se ha producido un error.
 * - RLF_ERR_CONN: No estaba conectado.
 * - RLF_ERR_TOOL: Formato incorrecto de la herramienta.
 * - RLF_ERR_MEM: Memoria insuficiente.
 */
int RLF_Finalize(int status, char * desc);

// Funciones de la herramienta:
/**
 * Obtiene una constante de la herramienta. La almacena en la estructura RLF_Data.
 * @param name Nombre de la constante.
 * @param data Dato donde almacenar.
 * @return Distinto de RLF_SUCCESS si se ha producido un error.
 * - RLF_ERR_CONN: No estaba conectado.
 * - RLF_ERR_TOOL: Formato incorrecto de la herramienta
 * - RLF_ERR_DATA: No se encuentra la constante.
 * - RLF_ERR_MEM: Memoria insuficiente.
 */
int RLF_GetConst(char * name, RLF_Data * data);
/**
 * Obtiene un atributo de la herramienta. Lo almacena en la estructura RLF_Data.
 * @param name Nombre del atributo.
 * @param data Dato donde almacenar.
 * @return Distinto de RLF_SUCCESS si se ha producido un error.
 * - RLF_ERR_CONN: No estaba conectado.
 * - RLF_ERR_TOOL: Formato incorrecto de la herramienta.
 * - RLF_ERR_DATA: No se encuentra el atributo.
 * - RLF_ERR_MEM: Memoria insuficiente.
 */
int RLF_GetAttribute(char * name, RLF_Data * data);
/**
 * Obtiene un parámetro asociado a la acción actual. Se almacena en la estructura RLF_Data.
 * @param name Nombre del parámetro.
 * @param data Dato donde almacenar.
 * @return Distinto de RLF_SUCCESS si se ha producido un error.
 * - RLF_ERR_CONN: No estaba conectado.
 * - RLF_ERR_TOOL: Formato incorrecto de la herramienta.
 * - RLF_ERR_DATA: No se encuentra el parámetro o no estaba asociado a la acción ejecutada.
 * - RLF_ERR_MEM: Memoria insuficiente.
 */
int RLF_GetParameter(char * name, RLF_Data * data);
/**
 * Modifica el parámetro de salida si está asignado a la acción.
 * @param name Nombre del parámetro.
 * @param value Valor.
 * @return Distinto de RLF_SUCCESS si se ha producido un error.
 * - RLF_ERR_CONN: No estaba conectado.
 * - RLF_ERR_TOOL: Formato incorrecto de la herramienta.
 * - RLF_ERR_DATA: No se encuentra el parámetro definido, no está asociado
 *                 a la acción o no es de salida.
 * - RLF_ERR_MEM: Memoria insuficiente.
 */
int RLF_SetParameter(char * name, char * value);
/**
 * Lanza una excepción de ejecución.
 * @param name Nombre de la excepción.
 * @param desc Descripción de la misma.
 * @return Distinto de RLF_SUCCESS si se ha producido un error.
 * - RLF_ERR_CONN: No estaba conectado.
 * - RLF_ERR_TOOL: Formato incorrecto de la herramienta.
 * - RLF_ERR_MEM: Memoria insuficiente.
 */
int RLF_ThrowException(char * name, char * desc);
