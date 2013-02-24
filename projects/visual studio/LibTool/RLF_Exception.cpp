// Clase RLF_Exception

#include "stdafx.h"
#include "LibTool.h"

using namespace LibTool;

// Constructor:
/*
 * Constructor de la excepción.
 * @param error Tipo de error.
 * @param msg Mensaje.
 */
RLF_Exception::RLF_Exception(RLF_Exception::Error error, String ^ msg):
	error(error),
	msg(msg)
	{}

// Métodos varios.
/*
 * GET error
 * @return Obtiene el error de la excepción.
 */
RLF_Exception::Error RLF_Exception::getError(){
	return this->error;
}

/*
 * GET msg
 * @return Mensaje de la excepción.
 */
String ^ RLF_Exception::getMsg(){
	return this->msg;
}