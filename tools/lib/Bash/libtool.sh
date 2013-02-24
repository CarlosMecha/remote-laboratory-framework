#!/bin/bash

#
# Remote Laboratory Framework
# 
# Librería para el acceso por parte de la herramienta al laboratorio. Es
# utilizada para obtener los parámetros introducidos por el usuario, así
# como las constantes y los atributos. También puede lanzar excepciones
# de ejecución y cambiar su estado de finalización.
#
# by Carlos A. Rodríguez Mecha
# v0.1
#
{
	
	# Constantes:
	RLF_SUCCESS=0;
	RLF_ERR_FILE=100;
	RLF_ERR_TOOL=101;
	RLF_ERR_CONN=102;
	RLF_ERR_KEY=103;
	RLF_ERR_DATA=104;
	RLF_ERR_ARGS=105;
	
	# Variables:
	SQLITE_COMM="sqlite3 -separator $";
	TOOL_FILE=`pwd`"/tool.rlf";
	RLF_CONNECTED=0;
	RLF_DATATOOL=0;
	RLF_ACTION="";
	
	# Estructura Rlf_Data:
	RLF_DATA_NAME="";
	RLF_DATA_VALUE="";
	RLF_DATA_MODIFIED="";
	RLF_DATA_MAX="";
	RLF_DATA_MIN="";
	RLF_DATA_DFL="";
	RLF_DATA_DTYPE="";
	
	# Funciones de conexión:
	
	#
	# Conecta el la herramienta con el laboratorio. Es necesario para 
	# poder utilizar todas las opciones del laboratorio.
	# @param $1 Clave proporcionada de la herramienta.
	# @return Distinto de 0 si no se ha podido conectar:
	# - RLF_ERR_FILE: La herramientan no ha sido registrada.
	# - RLF_ERR_TOOL: La herramientan no ha sido registrada.
	# - RLF_ERR_KEY: La clave introducida no corresponde con esta 
	#                herramienta.
	# - RLF_ERR_ARGS: Argumentos no válidos.
	#
	function RLF_Init(){
		
		# 1. Comprobación del fichero de la herramienta.
		if [ ! -e $TOOL_FILE ]; then {
			return $RLF_ERR_FILE;
		} fi;
		
		# 2. Comprobación.
		if [ "$1" == "" ]; then {
			return $RLF_ERR_ARGS;
		} fi;
		
		# 3. Comprobación del registro de la herramienta.
		INIT_VAL=`$SQLITE_COMM $TOOL_FILE "SELECT value FROM attribute WHERE name='INIT';"`;
		if [ "$INIT_VAL" != "true" ]; then {
			return $RLF_ERR_TOOL;
		} fi;
		
		unset INIT_VAL;
		
		# 4. Comprobación de la clave.
		KEY_VAL=`$SQLITE_COMM $TOOL_FILE "SELECT value FROM attribute WHERE name = 'KEY';"`;
		if [ "$KEY_VAL" != "$1" ]; then {
			return $RLF_ERR_KEY;
		} fi;
		
		unset KEY_VAL;
		
		# 5. Obtención de la acción en ejecución.
		RLF_ACTION=`$SQLITE_COMM $TOOL_FILE "SELECT value FROM attribute WHERE name='EXEC_ACTION';"`;
		if [ "$RLF_ACTION" == "" ]; then {
			return $RLF_ERR_TOOL;
		} fi;
		
		# 6. Obtención del tipo de herramienta.
		DATA=`$SQLITE_COMM $TOOL_FILE "SELECT value FROM attribute WHERE name='DATA';"`;
		if [ "$DATA" == "true" ]; then {
			RLF_DATATOOL=1;
		} fi;
		unset DATA;
		
		# 7. Actualización del estado.
		$SQLITE_COMM $TOOL_FILE "UPDATE attribute SET value = 'true' WHERE name = 'CONNECT';";
		
		RLF_CONNECTED=1;
		
		return $RLF_SUCCESS;
	}
	
	#
	# Desconecta de la herramienta del laboratorio. A partir de aquí no
	# se podrán guardar más parámetros de salida, excepciones ni estados.
	# @param $1 Valor de la salida de la acción (número).
	# @param $2 Descripción de la salida de la acción.
	# @return Distinto de 0 si se ha producido un error.
	# - RLF_ERR_CONN: No estaba conectado.
	# - RLF_ERR_DATA: El valor no es correcto.
	# - RLF_ERR_ARGS: Argumentos no válidos.
	#
	function RLF_Finalize(){
		
		# 1. Comprobación de la conexión.
		if [ ! $RLF_CONNECTED -eq 1 ]; then {
			return $RLF_ERR_CONN;
		} fi;
		
		# 2. Comprobación.
		if [ "$1" == "" ]; then {
			return $RLF_ERR_ARGS;
		} fi;
		if [ "$2" == "" ]; then {
			return $RLF_ERR_ARGS;
		} fi;
		
		# 3. Cierre de conexión.
		$SQLITE_COMM $TOOL_FILE "UPDATE attribute SET value = 'false' WHERE name = 'CONNECT';";
		
		# 4. Insercción del status.
		echo "INSERT INTO status (value, description, action) VALUES ("$1", '"$2"', '"$RLF_ACTION"');" > temp.sql;
		$SQLITE_COMM -init temp.sql $TOOL_FILE ".quit" 2> /dev/null;
		rm temp.sql;		

		if [ "$RLF_DATA" != "" ]; then {
			return $RLF_ERR_DATA;
		} fi;
		
		# 5. Liberar memoria.
		unset RLF_DATA;
		unset RLF_ACTION;
		unset RLF_CONNECTED;
		unset RLF_DATA_NAME;
		unset RLF_DATA_VALUE;
		unset RLF_DATA_MODIFIED;
		unset RLF_DATA_MAX;
		unset RLF_DATA_MIN;
		unset RLF_DATA_DFL;
		unset RLF_DATA_DTYPE;
		
		return $RLF_SUCCESS;
	}
	
	# Funciones de servicio:
	
	#
	# Obtiene una constante de la herramienta. La almacena en la estructura RLF_DATA.
	# @param $1 Nombre de la constante.
	# @return Distinto de 0 si se ha producido un error.
	# - RLF_ERR_CONN: No estaba conectado.
	# - RLF_ERR_DATA: No se encuentra la constante.
	# - RLF_ERR_ARGS: Argumentos no válidos.
	#
	function RLF_GetConst(){
		
		# 1. Comprobación de la conexión.
		if [ ! $RLF_CONNECTED -eq 1 ]; then {
			return $RLF_ERR_CONN;
		} fi;
		
		# 2. Comprobación.
		if [ "$1" == "" ]; then {
			return $RLF_ERR_ARGS;
		} fi;
		
		# 3. Consulta.
		RLF_DATA=`$SQLITE_COMM $TOOL_FILE "SELECT name, value, dtype FROM constant WHERE name = '"$1"';"`;
		if [ "$RLF_DATA" == "" ]; then {
			return $RLF_ERR_DATA;
		} fi;
		
		# 4. Datos.
		RLF_DATA_NAME=`echo $RLF_DATA | cut -d"$" -f 1`;
		RLF_DATA_VALUE=`echo $RLF_DATA | cut -d"$" -f 2`;
		RLF_DATA_DTYPE=`echo $RLF_DATA | cut -d"$" -f 3`;
		
		unset RLF_DATA;
		
		return $RLF_SUCCESS;
		
	}
	
	#
	# Obtiene un atributo de la herramienta. Lo almacena en la estructura
	# RLF_DATA.
	# @param $1 Nombre del atributo.
	# @return Distinto de 0 si se ha producido un error.
	# - RLF_ERR_CONN: No estaba conectado.
	# - RLF_ERR_DATA: No se encuentra el atributo.
	# - RLF_ERR_ARGS: Argumentos no válidos.
	#
	function RLF_GetAttribute(){
		
		# 1. Comprobación de la conexión.
		if [ ! $RLF_CONNECTED -eq 1 ]; then {
			return $RLF_ERR_CONN;
		} fi;
		
		# 2. Comprobación.
		if [ "$1" == "" ]; then {
			return $RLF_ERR_ARGS;
		} fi;
		
		# 3. Consulta.
		RLF_DATA=`$SQLITE_COMM $TOOL_FILE "SELECT name, value, dtype FROM attribute WHERE name = '"$1"';"`;
		if [ "$RLF_DATA" == "" ]; then {
			return $RLF_ERR_DATA;
		} fi;
		
		# 4. Datos.
		RLF_DATA_NAME=`echo $RLF_DATA | cut -d"$" -f 1`;
		RLF_DATA_VALUE=`echo $RLF_DATA | cut -d"$" -f 2`;
		RLF_DATA_DTYPE=`echo $RLF_DATA | cut -d"$" -f 3`;
		
		unset RLF_DATA;
		
		return $RLF_SUCCESS;
		
	}
	
	#
	# Obtiene un parámetro de la acción actual. Si no está asociado a la
	# acción en ejecución, se producirá un error. Se almacena en la
	# estructura RLF_DATA.
	# @param $1 Nombre del parámetro.
	# @return Distinto de 0 si se ha producido un error.
	# - RLF_ERR_CONN: No estaba conectado.
	# - RLF_ERR_DATA: No se encuentra el parámetro o no estaba asociado
	#                 a la acción en ejecución.
	# - RLF_ERR_ARGS: Argumentos no válidos.
	#
	function RLF_GetParameter(){
		
		# 1. Comprobación de la conexión.
		if [ ! $RLF_CONNECTED -eq 1 ]; then {
			return $RLF_ERR_CONN;
		} fi;
		
		# 2. Comprobación.
		if [ "$1" == "" ]; then {
			return $RLF_ERR_ARGS;
		} fi;
		
		# 3. Comprobación de acceso.
		if [ $RLF_DATATOOL -eq 1 ]; then {
			return $RLF_ERR_DATA;
		} fi;
		
		# 4. Consulta del parámetro.
		RLF_DATA=`$SQLITE_COMM $TOOL_FILE "SELECT p.name, p.value, p.dtype, p.min, p.max, p.dfl, p.modified FROM parameter p, action_parameter ap WHERE p.name = '"$1"' AND ap.parameter = p.name AND ap.action = '"$RLF_ACTION"';"`;
		
		if [ "$RLF_DATA" == "" ]; then {
			return $RLF_ERR_DATA;
		} fi;
		
		# 5. Datos.
		RLF_DATA_NAME=`echo $RLF_DATA | cut -d $ -f 1`;
		RLF_DATA_VALUE=`echo $RLF_DATA | cut -d $ -f 2`;
		RLF_DATA_DTYPE=`echo $RLF_DATA | cut -d $ -f 3`;
		RLF_DATA_MIN=`echo $RLF_DATA | cut -d $ -f 4`;
		RLF_DATA_MAX=`echo $RLF_DATA | cut -d $ -f 5`;
		RLF_DATA_DFL=`echo $RLF_DATA | cut -d $ -f 6`;
		RLF_DATA_MODIFIED=`echo $RLF_DATA | cut -d $ -f 7`;
		
		return $RLF_SUCCESS;
		
	}
	
	#
	# Modifica el parámetro de salida de la acción.
	# @param $1 Nombre del parámetro.
	# @param $2 Valor textual.
	# @return Distinto de 0 si se ha producido un error.
	# - RLF_ERR_CONN: No estaba conectado.
	# - RLF_ERR_DATA: No se encuentra el parámetro definido, no está
	#                 asociado a la acción o no es de salida.
	# - RLF_ERR_ARGS: Argumentos no válidos.
	#
	function RLF_SetParameter(){
		
		# 1. Comprobación de la conexión.
		if [ ! $RLF_CONNECTED -eq 1 ]; then {
			return $RLF_ERR_CONN;
		} fi;
		
		
		# 3. Comprobación de acceso.
		if [ $RLF_DATATOOL -eq 1 ]; then {
			return $RLF_ERR_DATA;
		} fi;
		
		# 4. Consulta.
		RLF_DATA=`$SQLITE_COMM $TOOL_FILE "SELECT p.name FROM parameter p, action_parameter ap WHERE p.name = '"$1"' AND ap.parameter = p.name AND ap.action = '"$RLF_ACTION"' AND NOT ap.parameter_type = 'in';"`;
		
		if [ "$RLF_DATA" == "" ]; then {
			return $RLF_ERR_DATA;
		} fi;
		
		# 5. Modificación.
		echo "UPDATE parameter SET value ='"$2"' WHERE name = '"$RLF_DATA"';" > temp.sql;
		$SQLITE_COMM -init temp.sql $TOOL_FILE ".quit" 2> /dev/null;
		rm temp.sql;
		
		unset RLF_DATA;
		
		return $RLF_SUCCESS;
		
	}
	
	#
	# Lanza una excepción de ejecución.
	# @param $1 Nombre de la excepción (string).
	# @param $2 Descripción de la misma.
	# @return Distinto de 0 si se ha producido un error.
	# - RLF_ERR_CONN: No estaba conectado.
	# - RLF_ERR_ARGS: Argumentos no válidos.
	#
	function RLF_ThrowException(){
		
		# 1. Comprobación de la conexión.
		if [ ! $RLF_CONNECTED -eq 1 ]; then {
			return $RLF_ERR_CONN;
		} fi;
		
		# 2. Comprobación.
		if [ "$1" == "" ]; then {
			return $RLF_ERR_ARGS;
		} fi;
		
		# 3. Insercción.
		echo "INSERT INTO exec_exception (name, description, action) VALUES ('"$1"', '"$2"', '"$RLF_ACTION"');" > temp.sql;
		$SQLITE_COMM -init temp.sql $TOOL_FILE ".quit" 2> /dev/null;
		rm temp.sql;
		
		# 4. Envío a stderr.
		echo "[EXCEPTION ("$1")] "$2 1>&2; 
		
		return $RLF_SUCCESS;
		
	}
	
	
}
