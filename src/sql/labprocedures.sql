--
-- Remote Laboratory Framework
--
-- by Carlos A. Rodríguez Mecha
--
-- Procedimientos de uso por parte de los laboratorios.
-- v0.1
--

DELIMITER $$

-- Identifica a un administrador. --
CREATE PROCEDURE authAdmin (IN admin_hash VARCHAR (45))

BEGIN
	SELECT COUNT(*) FROM user u, admin a WHERE u.user = a.name AND u.hash = admin_hash; 
END$$


-- Notifica una parada de emergencia de un laboratorio concreto. --
CREATE PROCEDURE emergency (in lab_name VARCHAR(20))
BEGIN
	
	UPDATE lab SET status = 5 WHERE name = lab_name;
	UPDATE tool SET status = 2 WHERE lab = lab_name;
	DELETE FROM registry WHERE tool = (SELECT id FROM tool WHERE lab = lab_name);
	
END$$


-- Obtiene una clave y un identificador para una nueva herramienta. --
CREATE PROCEDURE toolKey ()	

BEGIN
	UPDATE tool_sec SET value = value + 1;
	SELECT value AS id, SHA1(NOW()) AS toolkey FROM tool_sec; 
END$$


-- Elimina una herramienta del proveedor. --
CREATE PROCEDURE droptool (IN tid INTEGER)

BEGIN
	DELETE FROM registry WHERE tool = tid;
	DELETE FROM action_parameter WHERE tool = tid;
	DELETE FROM socket WHERE tool = tid;
	DELETE FROM parameter WHERE tool = tid;
	DELETE FROM action WHERE tool = tid;
	DELETE FROM constant WHERE tool = tid;
	DELETE FROM tool WHERE id = tid; 
END$$


-- Notifica el arranque de un laboratorio. --
CREATE PROCEDURE startlab (IN lab_name VARCHAR(20), IN p_request INTEGER, IN p_client INTEGER, IN p_notification INTEGER)	

BEGIN
	UPDATE lab SET port_request = p_request, port_client = p_client, port_notification = p_notification, status = 1 WHERE name = lab_name;
	UPDATE tool SET status = 2 WHERE lab = lab_name;
END$$


-- Notifica la para de un laboratorio. --
CREATE PROCEDURE stoplab (IN lab_name VARCHAR(20))

BEGIN
	DECLARE done INT DEFAULT 0;
	DECLARE id_tool INT;
	DECLARE cur CURSOR FOR SELECT id FROM tool WHERE lab = lab_name;
	DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET done = 1;

	UPDATE lab SET status = 2 WHERE name = lab_name;
	UPDATE tool SET status = 2 WHERE lab = lab_name;

	OPEN cur;
	REPEAT
		FETCH cur INTO id_tool;
		IF NOT done THEN
			DELETE FROM registry WHERE tool = id_tool;
		END IF;
	UNTIL done END REPEAT;

	CLOSE cur;
END$$


-- Activación de un laboratorio. --
CREATE PROCEDURE armlab (IN lab_name VARCHAR(20))

BEGIN
	UPDATE lab SET status = 3 WHERE name = lab_name;
	UPDATE tool SET status = 1 WHERE lab = lab_name;
END$$


-- Desactivación de un laboratorio. --
CREATE PROCEDURE disarmlab (IN lab_name VARCHAR(20))

BEGIN
	
	DECLARE done INT DEFAULT 0;
	DECLARE id_tool INT;
	DECLARE cur CURSOR FOR SELECT id FROM tool WHERE lab = lab_name;
	DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET done = 1;
	
	UPDATE lab SET status = 4 WHERE name = lab_name;
	UPDATE tool SET status = 2 WHERE lab = lab_name;
	
	OPEN cur;
	REPEAT
		FETCH cur INTO id_tool;
		IF NOT done THEN
			DELETE FROM registry WHERE tool = id_tool;
		END IF;
	UNTIL done END REPEAT;
	
	CLOSE cur;
	
	
	
END$$


-- Registra una desconexión por tiempo excedido. --
CREATE PROCEDURE timeout (IN client_token VARCHAR(40))

BEGIN
	
	DECLARE done INT DEFAULT 0;
	DECLARE id_tool INT;
	DECLARE cur CURSOR FOR SELECT tool FROM registry WHERE client = @client_name;
	DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET done = 1;
	
	SELECT name INTO @client_name FROM client WHERE token = client_token;
	UPDATE client SET token = NULL WHERE name = @client_name;

	OPEN cur;
	REPEAT
		FETCH cur INTO id_tool;
		IF NOT done THEN
			UPDATE tool SET status = 1 WHERE data = 0 AND id = id_tool;
		END IF;
	UNTIL done END REPEAT;
	CLOSE cur;
	
	DELETE FROM registry WHERE client = @client_name;
END$$


DELIMITER ;
