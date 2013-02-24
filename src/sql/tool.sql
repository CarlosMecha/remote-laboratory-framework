--
-- Remote Laboratory Framework
--
-- by Carlos A. Rodríguez Mecha
--
-- Estructura de una herramienta. Sintáxis de SQLite.
-- v0.1
--

-- Tabla que contiene los atributos propios de la herramienta. --
CREATE TABLE IF NOT EXISTS attribute (
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	name TEXT UNIQUE CHECK (length(name) BETWEEN 1 AND 128),
	value TEXT NOT NULL,
	dtype TEXT CHECK (dtype IN ('int', 'boolean', 'double', 'long', 'float', 'string', 'encode'))
);

-- Básicos. --
INSERT INTO attribute (name, value, dtype) VALUES ('ID', '0', 'int');
INSERT INTO attribute (name, value, dtype) VALUES ('PATH', '', 'string');
INSERT INTO attribute (name, value, dtype) VALUES ('KEY', '', 'string');
INSERT INTO attribute (name, value, dtype) VALUES ('NAME', '', 'string');
INSERT INTO attribute (name, value, dtype) VALUES ('DESCRIPTION', '', 'string');
INSERT INTO attribute (name, value, dtype) VALUES ('VERSION', '', 'string');
INSERT INTO attribute (name, value, dtype) VALUES ('ADMIN', '', 'string');
INSERT INTO attribute (name, value, dtype) VALUES ('ROLE', '', 'string');
INSERT INTO attribute (name, value, dtype) VALUES ('INSDATE', '', 'string');
INSERT INTO attribute (name, value, dtype) VALUES ('DATA', 'false', 'string');
INSERT INTO attribute (name, value, dtype) VALUES ('INSTREAM', 'false', 'boolean');
INSERT INTO attribute (name, value, dtype) VALUES ('OUTSTREAM', 'false', 'boolean');

-- De ejecución. --
INSERT INTO attribute (name, value, dtype) VALUES ('INIT', 'true', 'boolean');
INSERT INTO attribute (name, value, dtype) VALUES ('CONNECT', 'false', 'boolean');
INSERT INTO attribute (name, value, dtype) VALUES ('EXEC_ACTION', '', 'string');

-- Contiene las constantes de ejecución. --
CREATE TABLE IF NOT EXISTS constant (
	name TEXT PRIMARY KEY CHECK (length(name) <= 128),
	description TEXT CHECK (length(description) <= 500),
	value TEXT,
	dtype TEXT CHECK (dtype IN ('int', 'boolean', 'long', 'double', 'string', 'encode'))
);

-- Almacena las acciones de la herramienta. --
CREATE TABLE IF NOT EXISTS action (
	name TEXT PRIMARY KEY CHECK (length(name) <= 128),
	description TEXT CHECK (length(description) <= 500),
	value TEXT NOT NULL CHECK (length(value) <= 1024),
	timeout INTEGER NOT NULL CHECK (timeout >= 0)
);

-- Estado de la herramienta en ejecución. --
CREATE TABLE IF NOT EXISTS status (
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	value INTEGER NOT NULL,
	description TEXT CHECK (length(description) <= 500),
	status_date DATE DEFAULT (datetime('now')),
	action TEXT NOT NULL,
	FOREIGN KEY (action) REFERENCES action(name)
);

-- Excepciones de la herramienta en ejecución. --
CREATE TABLE IF NOT EXISTS exec_exception (
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	throw_date DATE DEFAULT (datetime('now')),
	name TEXT NOT NULL,
	description TEXT CHECK (length(description) <= 500),
	action TEXT INTEGER NOT NULL,
	FOREIGN KEY (action) REFERENCES action(name)
);

-- Tabla para la identificación de Sockets. --
CREATE TABLE IF NOT EXISTS socket (
	action TEXT NOT NULL,
	port INTEGER NOT NULL,
	protocol TEXT NOT NULL,
	type TEXT CHECK (type IN ('data', 'media', 'graph')),
	mode TEXT CHECK (mode IN ('r', 'w', 'rw')),
	PRIMARY KEY (action, port),
	FOREIGN KEY (action) REFERENCES action(name)
	
);

-- Tabla de Parámetros. --
CREATE TABLE IF NOT EXISTS parameter (
	name TEXT PRIMARY KEY CHECK (length(name) <= 128),
	description TEXT CHECK (length(description) <= 500),
	value TEXT,
	dfl TEXT,
	min TEXT,
	max TEXT,
	dtype TEXT CHECK (dtype IN ('int', 'boolean', 'long', 'double', 'string', 'encode')),
	modified INTEGER DEFAULT 0 CHECK (modified BETWEEN 0 AND 1)
);

-- Tabla de relación de parámetros con las acciones.
CREATE TABLE IF NOT EXISTS action_parameter (
	action TEXT NOT NULL,
	parameter TEXT NOT NULL,
	parameter_type TEXT CHECK (parameter_type IN ('in', 'out', 'inout')),
	PRIMARY KEY (action, parameter),
	FOREIGN KEY (action) REFERENCES action(name),
	FOREIGN KEY (parameter) REFERENCES parameter(name)
);


