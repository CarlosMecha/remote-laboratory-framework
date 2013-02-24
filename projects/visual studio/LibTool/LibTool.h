// LibTool.h

#pragma once

using namespace System;

namespace LibTool {

	public ref class RLF_Data
	{			
		
		public:
			// Enumeraciones:
			/* Tipos de dato. */
			enum class ObjectType{
				/* Atributo. */
				Attribute,
				/* Constante. */
				Const,
				/* Par�metro. */
				Parameter
			};
			/* Tipo de dato contenido. */
			enum class DataType{
				/* Tipo int. */
				DInt,
				/* Tipo boolean. */
				DBoolean,
				/* Tipo double. */
				DDouble,
				/* Tipo long. */
				DLong,
				/* Tipo string. */
				DString,
				/* Tipo codificado. */
				DEncode
			};

			// M�todos varios.
			/* Obtiene el tipo de dato. */
			RLF_Data::ObjectType getType();
			/* Nombre del dato. */
			String ^ getName();
			/* Indica si el dato ha sido modificado. */
			bool isModified();
			/* Valor del dato. */
			String ^ getValue();
			/* Valor m�ximo. */
			String ^ getMax();
			/* Valor m�nimo. */
			String ^ getMin();
			/* Valor por defecto. */
			String ^ getDfl();
			/* Tipo de dato contenido. */
			RLF_Data::DataType getDtype();

			// Constructores:
			/* Constructor completo. */
			RLF_Data(RLF_Data::ObjectType type, String ^ name, bool modified,
					String ^ value, String ^ max, String ^ min, String ^ dlf, RLF_Data::DataType dtype);
			/* Constructor para constantes y atributos. */
			RLF_Data(RLF_Data::ObjectType type, String ^ name, String ^ value, RLF_Data::DataType dtype);
			/* Destructor. */
			~RLF_Data();

			// Funciones:
			/* Convierte un String en un tipo determinado de datos. */
			static RLF_Data::DataType StringToDataType(String ^ s);

		private:
			// Atributos:
			/* Tipo de dato. */
			RLF_Data::ObjectType type;
			/* Nombre. */
			String ^ name;
			/* Indica si el dato ha sido modificado. */
			bool modified;
			/* Valor del dato. */
			String ^ value;
			/* Valor m�ximo. */
			String ^ max;
			/* Valor m�nimo. */
			String ^ min;
			/* Valor por defecto. */
			String ^ dfl;
			/* Tipo de dato contenido. */
			RLF_Data::DataType dtype;

	};

	

	/* Excepci�n RLF. */
	public ref class RLF_Exception
	{
		public:
			// Enums:
			/* Tipo de error. */
			enum class Error{
				/* No se encuentra la base de datos. */
				DBError,
				/* La herramienta a�n no ha sido dado de alta. */
				ToolError,
				/* Error con la conexi�n de la base de datos. */
				ConnError,
				/* Error con la identificaci�n de la herramienta. */
				KeyError,
				/* Error de datos. */
				DataError,
				/* Error de conversi�n de datos. */
				ConvError,
				/* Error desconocido. */
				UnknownError
			};
			
			// Constructore:
			/* Constructor. */
			RLF_Exception(RLF_Exception::Error error, String ^ msg);

			// M�todos varios:
			/* Obtiene el error de la excepci�n. */
			RLF_Exception::Error getError();
			/* Obtiene el mensaje de la excepci�n. */
			String ^ getMsg();

		private:

			// Atributos:
			/* Error. */
			RLF_Exception::Error error;
			/* Mensaje. */
			String ^ msg;

	};

	/* Gestor del servicio. */
	public ref class RLF_Manager
	{
		public:
			// Constantes:
			/* Archivo del servicio. */
			static const String ^ TOOL_FILE = "tool.rlf";
			
			// Constructor:
			/* Constructor. */
			RLF_Manager();

			// M�todos de la herramienta:
			/* Crea la conexi�n con el laboratorio. */
			void init(String ^ key);
			/* Desconecta del laboratorio. */
			void finalize(int status, String ^ desc);
			/* Obtiene una constante. */
			RLF_Data ^ getConst(String ^ name);
			/* Obtiene un atributo. */
			RLF_Data ^ getAttribute(String ^ name);
			/* Obtiene un par�metro de entrada. */
			RLF_Data ^ getParameter(String ^ name);
			/* Define un par�metro de salida. */
			void setParameter(String ^ name, String ^ value);
			/* Lanza una excepci�n. */
			void throwException(String ^ name, String ^ description);

			// M�todos varios:
			/* Indica si el manager est� conectado. */
			bool isConnected();

		private:
			/* Indica si est� conectado. */
			bool connected;
			/* Cadena de conexi�n. */
			String ^ connectionString;
			/* Comando actual. */
			String ^ command;
			/* Servicio multiacceso. */
			bool datatool;
	};



}
