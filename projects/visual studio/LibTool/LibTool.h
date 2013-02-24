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
				/* Parámetro. */
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

			// Métodos varios.
			/* Obtiene el tipo de dato. */
			RLF_Data::ObjectType getType();
			/* Nombre del dato. */
			String ^ getName();
			/* Indica si el dato ha sido modificado. */
			bool isModified();
			/* Valor del dato. */
			String ^ getValue();
			/* Valor máximo. */
			String ^ getMax();
			/* Valor mínimo. */
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
			/* Valor máximo. */
			String ^ max;
			/* Valor mínimo. */
			String ^ min;
			/* Valor por defecto. */
			String ^ dfl;
			/* Tipo de dato contenido. */
			RLF_Data::DataType dtype;

	};

	

	/* Excepción RLF. */
	public ref class RLF_Exception
	{
		public:
			// Enums:
			/* Tipo de error. */
			enum class Error{
				/* No se encuentra la base de datos. */
				DBError,
				/* La herramienta aún no ha sido dado de alta. */
				ToolError,
				/* Error con la conexión de la base de datos. */
				ConnError,
				/* Error con la identificación de la herramienta. */
				KeyError,
				/* Error de datos. */
				DataError,
				/* Error de conversión de datos. */
				ConvError,
				/* Error desconocido. */
				UnknownError
			};
			
			// Constructore:
			/* Constructor. */
			RLF_Exception(RLF_Exception::Error error, String ^ msg);

			// Métodos varios:
			/* Obtiene el error de la excepción. */
			RLF_Exception::Error getError();
			/* Obtiene el mensaje de la excepción. */
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

			// Métodos de la herramienta:
			/* Crea la conexión con el laboratorio. */
			void init(String ^ key);
			/* Desconecta del laboratorio. */
			void finalize(int status, String ^ desc);
			/* Obtiene una constante. */
			RLF_Data ^ getConst(String ^ name);
			/* Obtiene un atributo. */
			RLF_Data ^ getAttribute(String ^ name);
			/* Obtiene un parámetro de entrada. */
			RLF_Data ^ getParameter(String ^ name);
			/* Define un parámetro de salida. */
			void setParameter(String ^ name, String ^ value);
			/* Lanza una excepción. */
			void throwException(String ^ name, String ^ description);

			// Métodos varios:
			/* Indica si el manager está conectado. */
			bool isConnected();

		private:
			/* Indica si está conectado. */
			bool connected;
			/* Cadena de conexión. */
			String ^ connectionString;
			/* Comando actual. */
			String ^ command;
			/* Servicio multiacceso. */
			bool datatool;
	};



}
