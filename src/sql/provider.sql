--
-- Remote Laboratory Framework
--
-- by Carlos A. Rodríguez Mecha
--
-- Estructura de la base de datos del proveedor. Sintáxis de MySQL.
-- v0.1
--

-- Tabla que contiene la información de los distintos laboratorios dados de alta. --
CREATE TABLE IF NOT EXISTS lab (

	name VARCHAR(40) PRIMARY KEY,
	host VARCHAR(40) NOT NULL,
	port_request INTEGER CHECK (port_request > 0),
	port_client INTEGER CHECK (port_client > 0),
	port_notification INTEGER CHECK (port_notification > 0),
	description VARCHAR(400),
	status ENUM ('STARTED', 'STOPPED', 'ARMED', 'DISARMED', 'EMERGENCY') NOT NULL
	
);

CREATE INDEX ilab ON lab(name, host, port_request);

-- Usuarios básicos. --
CREATE TABLE IF NOT EXISTS user (

	user VARCHAR(25) PRIMARY KEY,
	hash_pass VARCHAR(40) NOT NULL,
	email VARCHAR(40) NOT NULL,
	hash VARCHAR(40) NOT NULL DEFAULT ''
	
);

-- Contiene los administradores del RLF. --
CREATE TABLE IF NOT EXISTS admin (

	name VARCHAR(40) PRIMARY KEY,
	tlf VARCHAR(40) NOT NULL,
	FOREIGN KEY (name) REFERENCES user ON DELETE CASCADE
	
);

-- Contiene los clientes del RLF. --
CREATE TABLE IF NOT EXISTS client (

	name VARCHAR(40) PRIMARY KEY,
	timeout INTEGER CHECK (timeout > 0),
	auth VARCHAR(40),
	monitor VARCHAR(40),
	token VARCHAR(40),
	role INTEGER CHECK (role >= 0),
	FOREIGN KEY (name) REFERENCES user ON DELETE CASCADE
	
);

CREATE INDEX iclient ON client(name, auth);

-- Secuencia de identificadores. --
CREATE TABLE IF NOT EXISTS tool_sec (
	value INTEGER PRIMARY KEY
);

INSERT INTO tool_sec(value) VALUES (10000000);

-- Lista de herramientas disponibles. --
CREATE TABLE IF NOT EXISTS tool (

	id INTEGER PRIMARY KEY,
	name VARCHAR(50) NOT NULL,
	toolkey VARCHAR(40) NOT NULL,
	description VARCHAR(2300) NOT NULL,
	version VARCHAR(20) NOT NULL,
	admin VARCHAR(40) NOT NULL,
	lab VARCHAR(40) NOT NULL,
	role INTEGER CHECK (role >= 0),
	ins_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	data INTEGER NOT NULL DEFAULT 0,
	instream INTEGER NOT NULL DEFAULT 0,
	outstream INTEGER NOT NULL DEFAULT 0,
	status ENUM ('ONLINE', 'OFFLINE', 'IN USE') NOT NULL,
	json TEXT,
	FOREIGN KEY (admin) REFERENCES admin ON DELETE RESTRICT,
	FOREIGN KEY (lab) REFERENCES lab ON DELETE CASCADE

);

-- Contiene las constantes de ejecución de cada herramienta. --
CREATE TABLE IF NOT EXISTS constant (
	tool INTEGER NOT NULL,
	name VARCHAR(128) NOT NULL,
	description VARCHAR(500) NOT NULL,
	value TEXT NOT NULL,
	dtype ENUM ('int', 'boolean', 'long', 'double', 'string', 'encode') NOT NULL,
	PRIMARY KEY (tool, name),
	FOREIGN KEY (tool) REFERENCES tool ON DELETE CASCADE
	
);

-- Almacena las acciones de la herramienta. --
CREATE TABLE IF NOT EXISTS action (
	tool INTEGER NOT NULL,
	name VARCHAR(128) NOT NULL,
	description VARCHAR(500) NOT NULL,
	timeout INTEGER NOT NULL CHECK (timeout >= 0),
	PRIMARY KEY (tool, name),
	FOREIGN KEY (tool) REFERENCES tool ON DELETE CASCADE
);

-- Tabla para la identificación de Sockets. --
CREATE TABLE IF NOT EXISTS socket (
	tool INTEGER NOT NULL,
	action VARCHAR(128) NOT NULL,
	port INTEGER NOT NULL,
	protocol VARCHAR(10) NOT NULL,
	type ENUM ('data', 'media', 'graph'),
	mode ENUM ('r', 'w', 'rw'),
	PRIMARY KEY (tool, action, port),
	FOREIGN KEY (tool, action) REFERENCES action(id_tool, name) ON DELETE CASCADE
	
);

-- Tabla de Parámetros. --
CREATE TABLE IF NOT EXISTS parameter (
	tool INTEGER NOT NULL,
	name VARCHAR(128) NOT NULL,
	description VARCHAR(500) NOT NULL,
	dfl TEXT,
	min TEXT,
	max TEXT,
	dtype ENUM ('int', 'boolean', 'long', 'double', 'string', 'encode') NOT NULL,
	PRIMARY KEY(tool, name),
	FOREIGN KEY(tool) REFERENCES tool ON DELETE CASCADE
);

-- Tabla de relación de parámetros con las acciones.
CREATE TABLE IF NOT EXISTS action_parameter (
	tool INTEGER NOT NULL,
	action VARCHAR(128) NOT NULL,
	parameter VARCHAR(128) NOT NULL,
	parameter_type ENUM ('in', 'out', 'inout'),
	PRIMARY KEY (tool, action, parameter),
	FOREIGN KEY (tool, action) REFERENCES action(tool, name) ON DELETE CASCADE, 
	FOREIGN KEY (tool, parameter) REFERENCES parameter(tool, name) ON DELETE CASCADE
);

-- Tabla con las reservas de los clientes. --
CREATE TABLE IF NOT EXISTS registry (
	tool INTEGER NOT NULL,
	client VARCHAR(25) NOT NULL,
	date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (tool, client),
	FOREIGN KEY (tool) REFERENCES tool ON DELETE CASCADE,
	FOREIGN KEY (client) REFERENCES client ON DELETE CASCADE
);
