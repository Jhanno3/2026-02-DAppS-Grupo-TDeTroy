-- Jugador.liga: dato identificatorio mas (misma categoria que club/posicion/nacionalidad,
-- V5__jugadores.sql) -- la liga en la que compite el jugador. NOT NULL igual que el resto de los
-- identificatorios; se agrega con un default transitorio para no romper filas ya persistidas y
-- se lo descarta de inmediato, asi que toda escritura nueva exige el valor explicitamente.
ALTER TABLE jugadores ADD COLUMN liga VARCHAR(100) NOT NULL DEFAULT 'Sin especificar';
ALTER TABLE jugadores ALTER COLUMN liga DROP DEFAULT;
