INSERT INTO lab (name, host, description) VALUES ('lab_01', '192.168.1.128', 'lab 1 Linux');
INSERT INTO lab (name, host, description) VALUES ('lab_02', '192.168.1.130', 'lab 2 Windows');

INSERT INTO user (user, hash_pass, email) VALUES ('carlos', SHA1('carlos'), 'k');
INSERT INTO user (user, hash_pass, email) VALUES ('javier', SHA1('javier'), 'k');
INSERT INTO user (user, hash_pass, email) VALUES ('ramon', SHA1('ramon'), 'k');
INSERT INTO admin (name, tlf) VALUES ('carlos', '123123');
INSERT INTO client (name, timeout, role) VALUES ('carlos', 15, 1);
INSERT INTO client (name, timeout, role) VALUES ('javier', 15, 1);
INSERT INTO client (name, timeout, role) VALUES ('ramon', 15, 1);
