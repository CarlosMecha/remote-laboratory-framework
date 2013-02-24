--
-- Remote Laboratory Framework
--
-- by Carlos A. Rodríguez Mecha
--
-- Base de datos del laboratorio.
-- v0.1
--

-- Tabla que contiene la información de las herramientas de un laboratorio concreto en ejecución. --
CREATE TABLE IF NOT EXISTS tool (
	id INTEGER PRIMARY KEY,
	path TEXT UNIQUE NOT NULL,
	key TEXT NOT NULL
);
