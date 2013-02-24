--
-- Remote Laboratory Framework
--
-- by Carlos A. Rodríguez Mecha
--
-- Disparadores.
-- v0.1
--

DELIMITER |

-- Crea el hash de identificación para los usuarios. --
CREATE TRIGGER ins_admin
BEFORE INSERT ON admin
FOR EACH ROW
BEGIN
	UPDATE user SET hash = SHA1(CONCAT(user, hash_pass)) WHERE user = NEW.name;
END
|

CREATE TRIGGER ins_client
BEFORE INSERT ON client
FOR EACH ROW
BEGIN
	UPDATE user SET hash = SHA1(CONCAT(user, hash_pass)) WHERE user = NEW.name;
END
|

DELIMITER ;
