// Clase RLF_Data

#include "stdafx.h"
#include "LibTool.h"

using namespace LibTool;
using namespace System::IO;
using namespace System::Text;

// Constructores:
/*
 * Constructor completo.
 * @param type Tipo del objeto.
 * @param name Nombre del dato.
 * @param modified Indica si el dato ha sido modificado.
 * @param value Valor del dato.
 * @param max Valor máximo.
 * @param min Valor mínimo.
 * @param dfl Valor por defecto.
 * @param dtype Tipo de valor.
 */
RLF_Data::RLF_Data(RLF_Data::ObjectType type, String ^ name, bool modified,
					String ^ value, String ^ max, String ^ min, String ^ dlf, RLF_Data::DataType dtype):
		type(type),
		name(name),
		modified(modified),
		value(value),
		max(max),
		min(min),
		dfl(dfl),
		dtype(dtype)
		{}

/*
 * Constructor para constantes y atributos.
 * @param type Tipo del objeto.
 * @param name Nombre del dato.
 * @param value Valor del dato.
 * @param dtype Tipo de valor.
 */
RLF_Data::RLF_Data(RLF_Data::ObjectType type, String ^ name, String ^ value, RLF_Data::DataType dtype):

		type(type),
		name(name),
		modified(false),
		value(value),
		max(nullptr),
		min(nullptr),
		dfl(nullptr),
		dtype(dtype)
		{}

/*
 * Destructor del objeto.
 */
RLF_Data::~RLF_Data(){}

// Métodos varios:
/* 
 * GET type
 * @return Tipo de objeto.
 */
RLF_Data::ObjectType RLF_Data::getType(){
	return this->type;
}

/* 
 * GET name
 * @return Nombre del dato.
 */
String ^ RLF_Data::getName(){
	return this->name;
}
			
/*
 * GET modified
 * @return Indica si el dato ha sido modificado.
 */
bool RLF_Data::isModified(){
	return this->modified;
}
			
/*
 * GET value
 * @return Valor del dato.
 */
String ^ RLF_Data::getValue(){
	return this->value;
}

/*
 * GET max
 * @return Valor máximo.
 */
String ^ RLF_Data::getMax(){
	return this->max;
}

/*
 * GET min
 * @return Valor mínimo.
 */
String ^ RLF_Data::getMin(){
	return this->min;
}

/*
 * GET dfl
 * @return Valor por defecto.
 */
String ^ RLF_Data::getDfl(){
	return this->dfl;
}
			
/*
 * GET dtype
 * @return Tipo de dato contenido.
 */
RLF_Data::DataType RLF_Data::getDtype(){
	return this->dtype;
}

// Funciones:
/*
 * Convierte un String a un tipo de datos.
 * @param s String.
 * @return Tipo de datos.
 * @throws RLF_Exception:
 * - ConvError: El string no es un tipo de datos.
 */
RLF_Data::DataType RLF_Data::StringToDataType(String ^ s){

	RLF_Data::DataType type;

	if (s->CompareTo("int") == 0) {
		type = RLF_Data::DataType::DInt;
	} else if (s->CompareTo("boolean") == 0) {
		type = RLF_Data::DataType::DBoolean;
	} else if (s->CompareTo("string") == 0) {
		type = RLF_Data::DataType::DString;
	} else if (s->CompareTo("long") == 0) {
		type = RLF_Data::DataType::DLong;
	} else if (s->CompareTo("double") == 0) {
		type = RLF_Data::DataType::DDouble;
	} else if (s->CompareTo("encode") == 0) {
		type = RLF_Data::DataType::DEncode;
	} else throw gcnew RLF_Exception(RLF_Exception::Error::ConvError, "El String no era un tipo.");

	return type;

}