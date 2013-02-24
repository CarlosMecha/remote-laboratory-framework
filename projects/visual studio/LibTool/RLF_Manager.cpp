// Clase RLF_Manager

#include "stdafx.h"
#include <stdlib.h>
#include <stdio.h>
#include <malloc.h>
#include "LibTool.h"

using namespace LibTool;
using namespace System::Data::SQLite;
using namespace System::Data::SqlClient;
using namespace System::IO;
using namespace System::Text;

// Constructor:
/*
 * Constructor del manager.
 */
RLF_Manager::RLF_Manager():
	connected(false),
	command(""),
	datatool(false){
	
	this->connectionString = "Data Source=" + RLF_Manager::TOOL_FILE + ";Pooling=true;FailIfMissing=true";
}

// M�todos del servicio:
/* 
 * Crea la conexi�n de la herramienta con el laboratorio.
 * @param key Clave proporcionada para la herramienta.
 * @throws RLF_Exception:
 * - DBError: La herramienta no ha sido registrada.
 * - ToolError: La herramienta no ha sido registrada.
 * - KeyError: La clave no es v�lida para esta herramienta.
 * - UnknownError: Error desconocido.
 */
void RLF_Manager::init(String ^ key){

	SQLiteConnection ^ conn;
	SQLiteCommand ^ cmd;
	bool initservice = false;
	String ^ sqltool = "SELECT value FROM attribute WHERE name='INIT'";
	String ^ sqlkey = "SELECT value FROM attribute WHERE name = 'KEY'";
	String ^ sqlconnection = "UPDATE attribute SET value = 'true' WHERE name = 'CONNECT'";
	String ^ sqlaction = "SELECT value FROM attribute WHERE name = 'EXEC_ACTION'";
	String ^ sqldata = "SELECT value FROM attribute WHERE name = 'DATA'";
	String ^ query;

	// 1. Conexi�n con la base de datos.
	try{
		conn = gcnew SQLiteConnection(this->connectionString);
		conn->Open();
	} catch (SqlException ^ e){
		throw gcnew RLF_Exception(RLF_Exception::Error::DBError, "No se ha podido conectar con la base de datos. " + e->Message);
	} catch (...){
		throw gcnew RLF_Exception(RLF_Exception::Error::UnknownError, "Error desconocido.");
	}

	// 2. Comprobaci�n de la creaci�n del servicio.
	try{
		cmd = gcnew SQLiteCommand(sqltool, conn);
		if(((String ^) cmd->ExecuteScalar())->CompareTo("true") == 0) initservice = true;
	} catch (SqlException ^ e){
		throw gcnew RLF_Exception(RLF_Exception::Error::ToolError, "La herramienta no ha sido activada. " + e->Message);
	} catch (Exception ^ e){
		throw gcnew RLF_Exception(RLF_Exception::Error::UnknownError, "Error desconocido 1." + e->Message);
	}

	if (!initservice){
		throw gcnew RLF_Exception(RLF_Exception::Error::ToolError, "La herramienta no ha sido activada. " + query->Length);
	}

	// 3. Comprobaci�n de la key.
	try{
		cmd = gcnew SQLiteCommand(sqlkey, conn);
		query = (String ^) cmd->ExecuteScalar();
		if (query->CompareTo(key) != 0) throw gcnew RLF_Exception(RLF_Exception::Error::KeyError, "La clave es incorrecta.");
	} catch (...){
		throw gcnew RLF_Exception(RLF_Exception::Error::UnknownError, "Error desconocido.");
	}

	// 4. Conexi�n.
	try{
		cmd = gcnew SQLiteCommand(sqlconnection, conn);
		cmd->ExecuteNonQuery();
		
	} catch (...){
		throw gcnew RLF_Exception(RLF_Exception::Error::UnknownError, "Error desconocido.");
	}
	
	// 5. Tipo de acceso.
	try{
		cmd = gcnew SQLiteCommand(sqldata, conn);
		if(((String ^) cmd->ExecuteScalar())->CompareTo("true") == 0) this->datatool = true;
	} catch (SqlException ^ e){
		throw gcnew RLF_Exception(RLF_Exception::Error::ToolError, "La herramienta no ha sido activada. " + e->Message);
	} catch (...){
		throw gcnew RLF_Exception(RLF_Exception::Error::UnknownError, "Error desconocido.");
	}

	// 6. Tipo de acceso.
	try{
		cmd = gcnew SQLiteCommand(sqlaction, conn);
		this->command = (String ^) cmd->ExecuteScalar();
	} catch (SqlException ^ e){
		throw gcnew RLF_Exception(RLF_Exception::Error::ToolError, "La herramienta no ha sido activada. " + e->Message);
	} catch (...){
		throw gcnew RLF_Exception(RLF_Exception::Error::UnknownError, "Error desconocido.");
	}

	conn->Close();
	this->connected = true;

}

/*
 * Desconecta del laboratorio e indica el estado de su finalizaci�n.
 * @param status Salida del programa.
 * @param desc Descripci�n del estado de salida.
 * @throws RLF_Exception:
 * - ConnError: No estaba conectado.
 * - UnknownError: Error desconocido.
 */
void RLF_Manager::finalize(int status, String ^ desc){
	
	String ^ sqlconnection = "UPDATE attribute SET value = 'false' WHERE name = 'CONNECT'";
	String ^ sqlstatus = "INSERT INTO status (value, description, action) VALUES (" + status + ", '" + desc + "', '" + this->command +"')";
	SQLiteConnection ^ conn;
	SQLiteCommand ^ cmd;

	// 1. Comprobaci�n de conexi�n.
	if (!this->connected) throw gcnew RLF_Exception(RLF_Exception::Error::ConnError, "No se ha conectado.");

	// 2. Aviso de la finalizaci�n.
	try{

		conn = gcnew SQLiteConnection(this->connectionString);
		conn->Open();
		cmd = gcnew SQLiteCommand(sqlconnection, conn);
		cmd->ExecuteNonQuery();
		cmd = gcnew SQLiteCommand(sqlstatus, conn);
		cmd->ExecuteNonQuery();
		conn->Close();

	} catch(...) {
		throw gcnew RLF_Exception(RLF_Exception::Error::UnknownError, "Error desconocido.");
	}

	this->connected = false;

}

/*
 * Obtiene una constante de la herramienta.
 * @param name Nombre de la constante.
 * @return Datos de la constante.
 * @throws RLF_Exception:
 * - ConnError: No estaba conectado.
 * - DataError: No existe esa constante.
 * - UnknownError: Error desconocido.
 */
RLF_Data ^ RLF_Manager::getConst(String ^ name){

	String ^ value;
	String ^ constant = "SELECT value, dtype FROM constant WHERE name='" + name + "'";
	SQLiteDataReader ^ reader;
	RLF_Data::DataType dtype;
	RLF_Data ^ data;
	SQLiteConnection ^ conn;
	SQLiteCommand ^ cmd;
	

	// 1. Comprobaci�n de conexi�n.
	if (!this->connected) throw gcnew RLF_Exception(RLF_Exception::Error::ConnError, "No se ha conectado.");

	// 2. Obtenci�n del dato.
	try{
		
		// 2.1 Apertura de la base de datos.
		conn = gcnew SQLiteConnection(this->connectionString);
		conn->Open();

		// 2.2 Consulta.
		cmd = gcnew SQLiteCommand(constant, conn);
		reader = cmd->ExecuteReader();
		if (reader->Read()){
			value = reader->GetString(0);
			dtype = RLF_Data::StringToDataType(reader->GetString(1));
		} else throw gcnew RLF_Exception(RLF_Exception::Error::DataError, "No existe esa constante.");

		// 2.3 Creaci�n del objeto.
		data = gcnew RLF_Data(RLF_Data::ObjectType::Const, name, value, dtype);

		reader->Close();
		conn->Close();

	} catch(SqlException ^ e) {
		throw gcnew RLF_Exception(RLF_Exception::Error::UnknownError, "Error desconocido. " + e->Message);
	}

	return data;

}

/*
 * Obtiene un atributo propio de la herramienta.
 * @param name Nombre del atributo.
 * @return Datos del atributo.
 * @throws RLF_Exception:
 * - ConnError: No estaba conectado.
 * - DataError: No existe ese atributo.
 * - UnknownError: Error desconocido.
 */
RLF_Data ^ RLF_Manager::getAttribute(String ^ name){

	String ^ value;
	String ^ constant = "SELECT value, dtype FROM attribute WHERE name='" + name + "'";
	SQLiteDataReader ^ reader;
	RLF_Data::DataType dtype;
	RLF_Data ^ data;
	SQLiteConnection ^ conn;
	SQLiteCommand ^ cmd;
	

	// 1. Comprobaci�n de conexi�n.
	if (!this->connected) throw gcnew RLF_Exception(RLF_Exception::Error::ConnError, "No se ha conectado.");

	// 2. Obtenci�n del dato.
	try{

		// 2.1 Apertura de la base de datos.
		conn = gcnew SQLiteConnection(this->connectionString);
		conn->Open();

		// 2.2 Consulta.
		cmd = gcnew SQLiteCommand(constant, conn);
		reader = cmd->ExecuteReader();
		if (reader->Read()){
			value = reader->GetString(0);
			dtype = RLF_Data::StringToDataType(reader->GetString(1));
		} else throw gcnew RLF_Exception(RLF_Exception::Error::DataError, "No existe ese atributo.");

		// 2.3 Creaci�n del objeto.
		data = gcnew RLF_Data(RLF_Data::ObjectType::Attribute, name, value, dtype);

		reader->Close();
		conn->Close();

	} catch(SqlException ^ e) {
		throw gcnew RLF_Exception(RLF_Exception::Error::UnknownError, "Error desconocido. " + e->Message);
	}

	return data;

}

/*
 * Obtiene un par�metro. Tiene que estar asignado para la acci�n en ejecuci�n.
 * @param name Nombre del par�metro.
 * @return Datos del par�metro.
 * @throws RLF_Exception:
 * - ConnError: No estaba conectado.
 * - DataError: No existe ese par�metro o puede que no est� asignado a la acci�n actual.
 * - UnknownError: Error desconocido.
 */
RLF_Data ^ RLF_Manager::getParameter(String ^ name){

	String ^ value, ^ max, ^ min, ^ dfl;
	bool modified;
	String ^ parameter = "SELECT p.name, p.value, p.dtype, p.min, p.max, p.dfl, p.modified FROM parameter p, action_parameter " +
							"ap WHERE p.name = '" + name + "' AND ap.parameter = p.name AND ap.action = '" + this->command + "'";
	SQLiteDataReader ^ reader;
	RLF_Data::DataType dtype;
	RLF_Data ^ data;
	SQLiteConnection ^ conn;
	SQLiteCommand ^ cmd;
	

	// 1. Comprobaci�n de conexi�n.
	if (!this->connected) throw gcnew RLF_Exception(RLF_Exception::Error::ConnError, "No se ha conectado.");

	// 2. Obtenci�n del dato.
	try{

		// 2.1 Apertura de la base de datos.
		conn = gcnew SQLiteConnection(this->connectionString);
		conn->Open();

		// 2.2 Consulta.
		cmd = gcnew SQLiteCommand(parameter, conn);
		reader = cmd->ExecuteReader();
		if (reader->Read()){
			value = reader->GetString(1);
			dtype = RLF_Data::StringToDataType(reader->GetString(2));
			min = reader->GetString(3);
			max = reader->GetString(4);
			dfl = reader->GetString(5);
			modified = reader->GetBoolean(6);
		} else throw gcnew RLF_Exception(RLF_Exception::Error::DataError, "No existe ese par�metro o no es un par�metro de entrada.");

		// 2.3 Creaci�n del objeto.
		data = gcnew RLF_Data(RLF_Data::ObjectType::Const, name, modified, value, max, min, dfl, dtype);

		reader->Close();

		cmd->ExecuteNonQuery();
		conn->Close();
		
	} catch(SqlException ^ e) {
		throw gcnew RLF_Exception(RLF_Exception::Error::UnknownError, "Error desconocido. " + e->Message);
	}

	return data;

}

/*
 * Define el valor de un par�metro. S�lo es posible con los par�metros de salida que est�n asociados a la acci�n actual.
 * @param name Nombre del par�metro
 * @param value Valor.
 * @throws RLF_Exception:
 * - ConnError: No estaba conectado.
 * - DataError: No existe ese par�metro o no es de salida. Puede que no est� asignado a la acci�n.
 * - UnknownError: Error desconocido.
 */
void RLF_Manager::setParameter(String ^ name, String ^ value){

	String ^ query = "SELECT p.name FROM parameter p, action_parameter ap WHERE p.name = '" + name + "' AND " +
						"ap.parameter = p.name AND cp.action = '" + this->command + "' AND NOT ap.parameter_type = 'in'";
	String ^ upd;
	int id;
	SQLiteConnection ^ conn;
	SQLiteCommand ^ cmd;
	SQLiteDataReader ^ reader;

	// 1. Comprobaci�n de conexi�n.
	if (!this->connected) throw gcnew RLF_Exception(RLF_Exception::Error::ConnError, "No se ha conectado.");

	// 2. Obtenci�n del ID.
	try{

		// 2.1 Apertura de la base de datos.
		conn = gcnew SQLiteConnection(this->connectionString);
		conn->Open();
		
		// 2.2 Consulta.
		cmd = gcnew SQLiteCommand(query, conn);
		reader = cmd->ExecuteReader();
		if (reader->Read()){
			id = reader->GetInt32(0);
		} else throw gcnew RLF_Exception(RLF_Exception::Error::DataError, "No existe ese par�metro o no es un par�metro de salida.");

		reader->Close();

		// 3. Actualizaci�n.
		upd = "UPDATE parameter SET value ='" + value + "' WHERE id = " + id;
		cmd = gcnew SQLiteCommand(upd, conn);
		cmd->ExecuteNonQuery();
		conn->Close();

	} catch(...) {
		throw gcnew RLF_Exception(RLF_Exception::Error::UnknownError, "Error desconocido.");
	}

}

/*
 * Lanza una excepci�n.
 * @param name Nombre de la excepci�n.
 * @param desc Descripci�n.
 * @throws RLF_Exception:
 * - ConnError: No estaba conectado.
 * - UnknownError: Error desconocido.
 */
void RLF_Manager::throwException(String ^ name, String ^ desc){

	String ^ query = "INSERT INTO exec_exception (name, description, action) VALUES ('" + name + "', '" + desc + "', '" + this->command + "');";
	SQLiteConnection ^ conn;
	SQLiteCommand ^ cmd;

	// 2. Acceso.
	try{

		// 2.1 Apertura de la base de datos.
		conn = gcnew SQLiteConnection(this->connectionString);
		conn->Open();

		// 2.2. Insercci�n.
		cmd = gcnew SQLiteCommand(query, conn);
		cmd->ExecuteNonQuery();
		conn->Close();
		
	} catch(...) {
		throw gcnew RLF_Exception(RLF_Exception::Error::UnknownError, "Error desconocido.");
	}

	// 3. Impresi�n.
	StreamWriter ^ stream = gcnew StreamWriter(Console::OpenStandardError());
	stream->AutoFlush = true;
	Console::SetError(stream);
	TextWriter ^ errorWriter = Console::Error;
    errorWriter->WriteLine("[EXCEPTION (" + name + ")] " + desc);

}

// M�todos varios:
/*
 * GET connected
 * @return Indica si el manager est� conectado.
 */
bool RLF_Manager::isConnected(){
	return this->connected;
}