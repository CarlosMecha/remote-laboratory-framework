// Clase RLF_Exception

#include "stdafx.h"
#include "LibTool.h"

using namespace LibTool;

// Constructor:
/*
 * Constructor de la excepci�n.
 * @param error Tipo de error.
 * @param msg Mensaje.
 */
RLF_Exception::RLF_Exception(RLF_Exception::Error error, String ^ msg):
	error(error),
	msg(msg)
	{}

// M�todos varios.
/*
 * GET error
 * @return Obtiene el error de la excepci�n.
 */
RLF_Exception::Error RLF_Exception::getError(){
	return this->error;
}

/*
 * GET msg
 * @return Mensaje de la excepci�n.
 */
String ^ RLF_Exception::getMsg(){
	return this->msg;
}