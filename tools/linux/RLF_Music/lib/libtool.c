/**
 * Remote Laboratory Framework
 *
 * Implementación de la librería para conectar la herramienta con el
 * laboratorio.
 * 
 * @author Carlos A. Rodríguez Mecha
 * @version 0.1
 */

// Librerías:
#include <string.h>
#include <sqlite3.h>
#include <stdlib.h>
#include <stdio.h>
#include "libtool.h"

// Definiciones:
#define TOOL "tool.rlf"

// Variables:
/** Indica si la herramienta está conectada. */
int rlf_connected = 0;
/** Identificador de la acción en ejecución */
char * rlf_action;
/** Indica si es una herramienta de datos. */
int rlf_datatool = 0;

// Funciones para los datos:
/**
 * Crea una nueva estructura vacía.
 * @return La estructura creada. Null si ha habido fallo de memoria.
 */
RLF_Data * RLF_Create(){
	
	RLF_Data * data;
	
	if ((data = (RLF_Data *) malloc(sizeof(RLF_Data))) == NULL) return NULL;
	data->name = NULL;
	data->value = NULL,
	data->dfl = NULL;
	data->min = NULL;
	data->max = NULL;
	data->ctype = attribute_type;
	data->dtype = string_type;
	data->modified = 0;
	
	return data;
}

/**
 * Destruye la estructura introducida.
 * @param data Estructura a destruir.
 */
void RLF_Destroy(RLF_Data * data){
	
	if (data == NULL) return;
	if (data->name != NULL) free(data->name);
	if (data->value != NULL) free(data->value);
	if (data->dfl != NULL) free(data->dfl);
	if (data->max != NULL) free(data->max);
	if (data->min != NULL) free(data->min);
	
}

// Funciones de comunicación:
/**
 * Conecta la herramienta con el laboratorio.
 * @param key Clave asignada a la herramienta.
 * @return Distinto de RLF_SUCCESS si no se ha podido conectar:
 * - RLF_ERR_CONN: Ya estaba conectado.
 * - RLF_ERR_FILE: La herramienta no ha sido registrada.
 * - RLF_ERR_TOOL: La herramienta no ha sido registrada.
 * - RLF_ERR_KEY: La clave introducida no es correcta.
 * - RLF_ERR_MEM: No hay memoria suficiente.
 */
int RLF_Init(char * key){
	
	sqlite3 * db;
	sqlite3_stmt * res;
	const char * next;
	char * error, * value;
	int len;
	const char * sqlinit = "SELECT value FROM attribute WHERE name='INIT';";
	const char * sqlkey = "SELECT value FROM attribute WHERE name = 'KEY';";
	const char * sqlconn = "UPDATE attribute SET value = 'true' WHERE name = 'CONNECT';";
	const char * sqlaction = "SELECT value FROM attribute WHERE name='EXEC_ACTION';";
	const char * sqldatatool = "SELECT value FROM attribute WHERE name='DATA';";
	
	// 0. Comprobación
	if (rlf_connected) return RLF_ERR_CONN;
	
	// 1. Conexión.
	if(sqlite3_open_v2(TOOL, &db, SQLITE_OPEN_READWRITE, NULL) != SQLITE_OK){
		return RLF_ERR_FILE;
	}
	
	// 2. Comprobación de la creación de la herramienta.
	if(sqlite3_prepare(db, sqlinit, strlen(sqlinit) * sizeof(char), &res, &next) != SQLITE_OK){
		sqlite3_close(db);
		return RLF_ERR_TOOL;
	}
	
	if (sqlite3_step(res) != SQLITE_ROW) return RLF_ERR_TOOL;
	value = (char *) sqlite3_column_text(res, 0);
	if (strcmp(value, "true")) {
		sqlite3_finalize(res);
		sqlite3_close(db);
		return RLF_ERR_TOOL;
	}
	
	sqlite3_finalize(res);
	
	// 3. Comprobación de la clave:
	if(sqlite3_prepare(db, sqlkey, strlen(sqlkey) * sizeof(char), &res, &next) != SQLITE_OK){
		return RLF_ERR_TOOL;
	}
	
	if (sqlite3_step(res) != SQLITE_ROW) return RLF_ERR_TOOL;
	value = (char *) sqlite3_column_text(res, 0);
	if (strcmp(value, key)) {
		sqlite3_finalize(res);
		sqlite3_close(db);
		return RLF_ERR_KEY;
	}
	
	sqlite3_finalize(res);
	
	// 4. Actualización del estado.
	if(sqlite3_exec(db , sqlconn, NULL, NULL, &error) != SQLITE_OK){
		sqlite3_close(db);
		return RLF_ERR_KEY;
	}
	
	// 5. Obtención de la acción.
	if(sqlite3_prepare(db, sqlaction, strlen(sqlaction) * sizeof(char), &res, &next) != SQLITE_OK){
		sqlite3_close(db);
		return RLF_ERR_TOOL;
	}
	
	if (sqlite3_step(res) != SQLITE_ROW) return RLF_ERR_TOOL;
	value = (char *) sqlite3_column_text(res, 0);
	len = strlen(value);
	if ((rlf_action = malloc((len + 1) * sizeof(char))) == NULL){
		sqlite3_close(db);
		return RLF_ERR_MEM;
	}
	strcpy(rlf_action, value);
	sqlite3_finalize(res);
	
	// 6. Herramienta de datos.
	if(sqlite3_prepare(db, sqldatatool, strlen(sqldatatool) * sizeof(char), &res, &next) != SQLITE_OK){
		sqlite3_close(db);
		return RLF_ERR_TOOL;
	}
	
	if (sqlite3_step(res) != SQLITE_ROW) return RLF_ERR_TOOL;
	value = (char *) sqlite3_column_text(res, 0);
	if (!strcmp(value, "true")) rlf_datatool = 1;
	sqlite3_finalize(res);
	
	rlf_connected = 1;
	
	sqlite3_close(db);
	
	return RLF_SUCCESS;
	
}

/**
 * Desconecta la herramienta en ejecución del laboratorio.
 * @param status Valor de la salida de la acción.
 * @param desc Descripción de la salida de la acción.
 * @return Distinto de RLF_SUCCESS si se ha producido un error.
 * - RLF_ERR_CONN: No estaba conectado.
 * - RLF_ERR_TOOL: Formato incorrecto de la herramienta.
 * - RLF_ERR_MEM: Memoria insuficiente.
 */
int RLF_Finalize(int status, char * desc){
	
	sqlite3 * db;
	const char * sqlconn = "UPDATE attribute SET value = 'false' WHERE name = 'CONNECT';";
	const char * sqlstatus = "INSERT INTO status (value, description, action) VALUES (%d, '%s', '%s');";
	char * buffer, * error;
	
	// 1. Comprobación de la conexión.
	if (!rlf_connected) return RLF_ERR_CONN;
	if(sqlite3_open_v2(TOOL, &db, SQLITE_OPEN_READWRITE, NULL) != SQLITE_OK){
		return RLF_ERR_CONN;
	}
	
	// 2. Cierre de conexión.
	if(sqlite3_exec(db , sqlconn, NULL, NULL, &error) != SQLITE_OK){
		sqlite3_close(db);
		return RLF_ERR_TOOL;
	}
		
	// 3. Insercción del estado.
	if ((buffer = malloc((strlen(desc) + strlen(sqlstatus)) * sizeof(char))) == NULL) return RLF_ERR_MEM;
	sprintf(buffer, sqlstatus, status, desc, rlf_action);
	if(sqlite3_exec(db , buffer, NULL, NULL, &error) != SQLITE_OK){
		sqlite3_close(db);
		return RLF_ERR_TOOL;
	}
	
	free(buffer);
	sqlite3_close(db);
	
	return RLF_SUCCESS;
	
}

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
int RLF_GetConst(char * name, RLF_Data * data){
	
	sqlite3 * db;
	sqlite3_stmt * res;
	const char * next, * dtype, * valuebuffer;
	const char * sqlselect = "SELECT value, dtype FROM constant WHERE name = '%s';";
	char * buffer; 
	
	// 1. Comprobación de la conexión.
	if (!rlf_connected) return RLF_ERR_CONN;
	if(sqlite3_open_v2(TOOL, &db, SQLITE_OPEN_READWRITE, NULL) != SQLITE_OK){
		return RLF_ERR_CONN;
	}
	
	// 2. Consulta.
	if ((buffer = malloc((strlen(name) + strlen(sqlselect)) * sizeof(char))) == NULL) return RLF_ERR_MEM;
	sprintf(buffer, sqlselect, name);
	if(sqlite3_prepare(db, buffer, strlen(buffer) * sizeof(char), &res, &next) != SQLITE_OK){
		sqlite3_close(db);
		return RLF_ERR_TOOL;
	}
	
	// 3. Utilización del dato.
	if (sqlite3_step(res) != SQLITE_ROW) {
		sqlite3_finalize(res);
		sqlite3_close(db);
		return RLF_ERR_DATA;
	}
	
	// 3.1 Obtención de los campos.
	valuebuffer = (const char *) sqlite3_column_text(res, 0);
	dtype = (const char *) sqlite3_column_text(res, 1);
	
	// 3.2 Preparación.
	if ((data->name = (char *) malloc((strlen(name) + 1) * sizeof(char))) == NULL){
		sqlite3_finalize(res);
		sqlite3_close(db);
		return RLF_ERR_MEM;
	} else strcpy(data->name, name);
	if (valuebuffer != NULL){
		if ((data->value = (char *) malloc((strlen(valuebuffer) + 1) * sizeof(char))) == NULL){
			sqlite3_finalize(res);
			sqlite3_close(db);
			return RLF_ERR_MEM;
		} else strcpy(data->value, valuebuffer);
	}
	
	// 3.3 Creación.
	if (!strcmp(dtype, "string")) data->dtype = string_type;
	else if (!strcmp(dtype, "int")) data->dtype = int_type;
	else if (!strcmp(dtype, "long")) data->dtype = long_type;
	else if (!strcmp(dtype, "boolean")) data->dtype = boolean_type;
	else if (!strcmp(dtype, "double")) data->dtype = double_type;
	else data->dtype = encode_type;
	data->ctype = constant_type;
	
	sqlite3_finalize(res);
	free(buffer);
	sqlite3_close(db);
	
	return RLF_SUCCESS;
	
}
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
int RLF_GetAttribute(char * name, RLF_Data * data){
	
	sqlite3 * db;
	sqlite3_stmt * res;
	const char * next, * dtype, * valuebuffer;
	const char * sqlselect = "SELECT value, dtype FROM attribute WHERE name = '%s';";
	char * buffer;
	
	// 1. Comprobación de la conexión.
	if (!rlf_connected) return RLF_ERR_CONN;
	if(sqlite3_open_v2(TOOL, &db, SQLITE_OPEN_READWRITE, NULL) != SQLITE_OK){
		return RLF_ERR_CONN;
	}
	
	// 2. Consulta.
	if ((buffer = malloc((strlen(name) + strlen(sqlselect)) * sizeof(char))) == NULL) return RLF_ERR_MEM;
	sprintf(buffer, sqlselect, name);
	if(sqlite3_prepare(db, buffer, strlen(buffer) * sizeof(char), &res, &next) != SQLITE_OK){
		sqlite3_close(db);
		return RLF_ERR_TOOL;
	}
	
	// 3. Utilización del dato.
	if (sqlite3_step(res) != SQLITE_ROW) {
		sqlite3_finalize(res);
		sqlite3_close(db);
		return RLF_ERR_DATA;
	}
	
	// 3.1 Obtención de los campos.
	valuebuffer = (const char *) sqlite3_column_text(res, 0);
	dtype = (const char *) sqlite3_column_text(res, 1);
	
	// 3.2 Preparación.
	
	if ((data->name = (char *) malloc((strlen(name) + 1) * sizeof(char))) == NULL){
		sqlite3_finalize(res);
		sqlite3_close(db);
		return RLF_ERR_MEM;
	} else strcpy(data->name, name);
	if (valuebuffer != NULL){
		if ((data->value = (char *) malloc((strlen(valuebuffer) + 1) * sizeof(char))) == NULL){
			sqlite3_finalize(res);
			sqlite3_close(db);
			return RLF_ERR_MEM;
		} else strcpy(data->value, valuebuffer);
	}
	
	// 3.3 Creación.
	if (!strcmp(dtype, "string")) data->dtype = string_type;
	else if (!strcmp(dtype, "int")) data->dtype = int_type;
	else if (!strcmp(dtype, "long")) data->dtype = long_type;
	else if (!strcmp(dtype, "boolean")) data->dtype = boolean_type;
	else if (!strcmp(dtype, "double")) data->dtype = double_type;
	else data->dtype = encode_type;
	data->ctype = attribute_type;
	
	sqlite3_finalize(res);
	free(buffer);
	sqlite3_close(db);
	
	return RLF_SUCCESS;
	
}

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
int RLF_GetParameter(char * name, RLF_Data * data){
	
	sqlite3 * db;
	sqlite3_stmt * res;
	const char * next, * dtype, * valuebuffer, * dflbuffer, * maxbuffer, * minbuffer;
	const char * sqlselect = "SELECT p.value, p.dtype, p.min, p.max, p.dfl, p.modified FROM parameter p, action_parameter ap WHERE p.name = '%s' AND ap.action = '%s' AND ap.parameter = p.name;";
	char * buffer;
	
	// 1. Comprobación de la conexión.
	if (!rlf_connected) return RLF_ERR_CONN;
	if (rlf_datatool) return RLF_ERR_DATA;
	if(sqlite3_open_v2(TOOL, &db, SQLITE_OPEN_READWRITE, NULL) != SQLITE_OK){
		return RLF_ERR_CONN;
	}
	
	// 2. Consulta.
	if ((buffer = malloc((strlen(name) + strlen(sqlselect)) * sizeof(char))) == NULL) return RLF_ERR_MEM;
	sprintf(buffer, sqlselect, name, rlf_action);
	if(sqlite3_prepare(db, buffer, strlen(buffer) * sizeof(char), &res, &next) != SQLITE_OK){
		sqlite3_close(db);
		return RLF_ERR_TOOL;
	}
	
	// 3. Utilización del dato.
	if (sqlite3_step(res) != SQLITE_ROW) {
		sqlite3_finalize(res);
		sqlite3_close(db);
		return RLF_ERR_DATA;
	}
	
	// 3.1 Obtención de los campos.
	valuebuffer = (const char *) sqlite3_column_text(res, 0);
	dtype = (const char *) sqlite3_column_text(res, 1);
	minbuffer = (const char *) sqlite3_column_text(res, 2);
	maxbuffer = (const char *) sqlite3_column_text(res, 3);
	dflbuffer = (const char *) sqlite3_column_text(res, 4);
	
	// 3.2 Preparación.
	if ((data->name = (char *) malloc((strlen(name) + 1) * sizeof(char))) == NULL){
		sqlite3_finalize(res);
		sqlite3_close(db);
		return RLF_ERR_MEM;
	} else strcpy(data->name, name);
	if (valuebuffer != NULL){
		if ((data->value = (char *) malloc((strlen(valuebuffer) + 1) * sizeof(char))) == NULL){
			sqlite3_finalize(res);
			sqlite3_close(db);
			return RLF_ERR_MEM;
		} else strcpy(data->value, valuebuffer);
	}
	if (minbuffer != NULL){
		if ((data->min = (char *) malloc((strlen(minbuffer) + 1) * sizeof(char))) == NULL){
			sqlite3_finalize(res);
			sqlite3_close(db);
			return RLF_ERR_MEM;
		} else strcpy(data->min, minbuffer);
	}
	if (maxbuffer != NULL){
		if ((data->max = (char *) malloc((strlen(maxbuffer) + 1) * sizeof(char))) == NULL){
			sqlite3_finalize(res);
			sqlite3_close(db);
			return RLF_ERR_MEM;
		} else strcpy(data->max, maxbuffer);
	}
	if (dflbuffer != NULL){
		if ((data->dfl = (char *) malloc((strlen(dflbuffer) + 1) * sizeof(char))) == NULL){
			sqlite3_finalize(res);
			sqlite3_close(db);
			return RLF_ERR_MEM;
		} else strcpy(data->dfl, dflbuffer);
	}
	
	// 3.3 Creación.
	if (!strcmp(dtype, "string")) data->dtype = string_type;
	else if (!strcmp(dtype, "int")) data->dtype = int_type;
	else if (!strcmp(dtype, "long")) data->dtype = long_type;
	else if (!strcmp(dtype, "boolean")) data->dtype = boolean_type;
	else if (!strcmp(dtype, "double")) data->dtype = double_type;
	else data->dtype = encode_type;
	data->ctype = parameter_type;
	data->modified = sqlite3_column_int(res, 5);
	
	sqlite3_finalize(res);
	free(buffer);
	sqlite3_close(db);
	
	return RLF_SUCCESS;
	
}

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
int RLF_SetParameter(char * name, char * value){
	
	sqlite3 * db;
	sqlite3_stmt * res;
	const char * next;
	const char * sqlselect = "SELECT p.name FROM parameter p, action_parameter ap WHERE p.name = '%s' AND cp.parameter = p.name AND cp.action = '%s' AND NOT ap.parameter_type = 'in';";
	const char * sqlupdate = "UPDATE parameter SET value = '%s' WHERE name = '%s';";
	char * buffer, * error;
	
	// 1. Comprobación de la conexión.
	if (!rlf_connected) return RLF_ERR_CONN;
	if (rlf_datatool) return RLF_ERR_DATA;
	if(sqlite3_open_v2(TOOL, &db, SQLITE_OPEN_READWRITE, NULL) != SQLITE_OK){
		return RLF_ERR_CONN;
	}
	
	// 2. Consulta del parámetro.
	if ((buffer = malloc((strlen(name) + strlen(sqlselect)) * sizeof(char))) == NULL) return RLF_ERR_MEM;
	sprintf(buffer, sqlselect, name, rlf_action);
	if(sqlite3_prepare(db, buffer, strlen(buffer) * sizeof(char), &res, &next) != SQLITE_OK){
		sqlite3_close(db);
		return RLF_ERR_TOOL;
	}
	
	// 3. Comprobación del parámetro.
	if (sqlite3_step(res) != SQLITE_ROW) {
		sqlite3_finalize(res);
		sqlite3_close(db);
		return RLF_ERR_DATA;
	}
	sqlite3_finalize(res);
	free(buffer);
	
	// 4. Actualización del parámetro.
	if ((buffer = malloc((strlen(value) + strlen(sqlupdate)) * sizeof(char))) == NULL) return RLF_ERR_MEM;
	sprintf(buffer, sqlupdate, value, name);
	if(sqlite3_exec(db , buffer, NULL, NULL, &error) != SQLITE_OK){
		sqlite3_close(db);
		return RLF_ERR_TOOL;
	}
	
	sqlite3_close(db);
	
	return RLF_SUCCESS;
	
}

/**
 * Lanza una excepción de ejecución.
 * @param name Nombre de la excepción.
 * @param desc Descripción de la misma.
 * @return Distinto de RLF_SUCCESS si se ha producido un error.
 * - RLF_ERR_CONN: No estaba conectado.
 * - RLF_ERR_TOOL: Formato incorrecto de la herramienta.
 * - RLF_ERR_MEM: Memoria insuficiente.
 */
int RLF_ThrowException(char * name, char * desc){
	
	sqlite3 * db;
	const char * sqlexception = "INSERT INTO exec_exception (name, description, action) VALUES ('%s', '%s', '%s');";
	char * buffer, * error;
	
	// 1. Comprobación de la conexión.
	if (!rlf_connected) return RLF_ERR_CONN;
	if(sqlite3_open_v2(TOOL, &db, SQLITE_OPEN_READWRITE, NULL) != SQLITE_OK){
		return RLF_ERR_CONN;
	}
	
	// 2. Impresión por stderr.
	fprintf(stderr, "[EXCEPTION (%s)] %s\n", name, desc);
	fflush(stderr);
	
	// 3. Insercción de la excepción.
	if ((buffer = malloc((strlen(name) + strlen(desc) + strlen(sqlexception)) * sizeof(char))) == NULL) return RLF_ERR_MEM;
	sprintf(buffer, sqlexception, name, desc, rlf_action);
	if(sqlite3_exec(db , buffer, NULL, NULL, &error) != SQLITE_OK){
		sqlite3_close(db);
		return RLF_ERR_TOOL;
	}
	
	free(buffer);
	sqlite3_close(db);
	
	return RLF_SUCCESS;
	
}
