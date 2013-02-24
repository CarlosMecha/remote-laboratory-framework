--
-- Remote Laboratory Framework
--
-- by Carlos A. Rodríguez Mecha
--
-- Procedimientos de uso por parte del servicio web.
-- v0.1
--


DELIMITER $$


-- Verifica el token de acceso de un usuario devolviendo su nombre. --
CREATE PROCEDURE verifytoken (IN client_auth VARCHAR (40))

BEGIN
	SELECT c.name FROM user u, client c WHERE u.user = c.name AND c.auth = client_auth; 
END$$

-- Verifica el token de acceso de un monitor devolviendo el nombre de su usuario. --
CREATE PROCEDURE verifymonitortoken (IN client_monitor VARCHAR (40))

BEGIN
	SELECT c.name FROM user u, client c WHERE u.user = c.name AND c.monitor = client_monitor; 
END$$

-- Obtiene un token y el tiempo máximo de uso para un usuario dado. --
CREATE PROCEDURE usetoken (IN client_name VARCHAR (40))

BEGIN
	UPDATE client SET token = SHA1(CONCAT(client_name, NOW())) WHERE name = client_name;
	SELECT token, timeout FROM client WHERE name = client_name;
END$$


-- Identifica a un cliente devolviendo su nuevo token de acceso. --
CREATE PROCEDURE loginClient (IN client_hash VARCHAR (40))	

BEGIN
	SELECT c.name INTO @client_name FROM user u, client c WHERE u.user = c.name AND u.hash = client_hash AND c.auth IS NULL;
	UPDATE client SET auth = SHA1(CONCAT(@client_name, NOW())) WHERE name = @client_name;
	SELECT auth FROM client WHERE name = @client_name;
END$$

-- Identifica a un monitor devolviendo su nuevo token de acceso. --
CREATE PROCEDURE loginMonitor (IN client_hash VARCHAR (40))	

BEGIN
	SELECT c.name INTO @client_name FROM user u, client c WHERE u.user = c.name AND u.hash = client_hash;
	UPDATE client SET monitor = SHA1(CONCAT(@client_name, NOW())) WHERE name = @client_name;
	SELECT monitor FROM client WHERE name = @client_name;
END$$

-- Desconecta a un cliente. --
CREATE PROCEDURE logoutClient (IN client_name VARCHAR (40))

BEGIN
	
	DECLARE done INT DEFAULT 0;
	DECLARE id_tool INT;
	DECLARE cur CURSOR FOR SELECT tool FROM registry WHERE client = client_name;
	DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET done = 1;

	UPDATE client SET auth = NULL, token = NULL WHERE name = client_name;
	
	OPEN cur;
	REPEAT
		FETCH cur INTO id_tool;
		IF NOT done THEN
			UPDATE tool SET status = 1 WHERE data = 0 AND id = id_tool;
		END IF;
	UNTIL done END REPEAT;
	CLOSE cur;
	
	DELETE FROM registry WHERE client = client_name;
END$$

-- Desconecta a un monitor. --
CREATE PROCEDURE logoutMonitor (IN client_name VARCHAR (40))

BEGIN
	UPDATE client SET monitor = NULL WHERE name = client_name;
END$$

-- Obtiene el estado de las herramientas a las que puede acceder el usuario. --
CREATE PROCEDURE status (IN client_name VARCHAR (40))

BEGIN
	SELECT role INTO @client_role FROM client WHERE name = client_name; 
	SELECT id, name, status FROM tool WHERE role <= @client_role;
END$$

-- Obtiene las definiciones en formato Json de las herramientas a las que puede acceder el usuario. --
CREATE PROCEDURE definitions (IN client_name VARCHAR (40))

BEGIN
	SELECT role INTO @client_role FROM client WHERE name = client_name; 
	SELECT json FROM tool WHERE role <= @client_role;
END$$

-- Reserva una herramienta. Devuelve la dirección del laboratorio asociado. --
CREATE PROCEDURE takeTool (IN client_name VARCHAR(40), IN id_tool INTEGER)

BEGIN

	UPDATE tool SET status = 3 WHERE data = 0 AND id = id_tool;
	INSERT INTO registry (tool, client) VALUES (id_tool, client_name);
	SELECT l.name AS name, l.host AS host, l.port_request AS port, l.port_client AS pclient, l.port_notification AS pnotification FROM lab l, tool t WHERE l.status = 3 AND l.name = t.lab AND t.id = id_tool;

END$$

-- Obtiene la lista de las herramientas seleccionadas por un usuario así como la dirección de sus laboratorios. --
CREATE PROCEDURE usedTools(IN client_name VARCHAR(40), OUT useToken VARCHAR(40))

BEGIN
	SELECT token INTO useToken FROM client WHERE name = client_name;
	SELECT r.tool AS tool, l.host AS host, l.port_request AS port FROM tool t, lab l, registry r WHERE l.name = t.lab AND t.id = r.tool AND r.client = client_name;
END$$

DELIMITER ;
