-- V23: Ponderacion de metricas por posicion de jugador (Arquero, Defensor, Centrocampista, Delantero)
-- y configuracion dinamica de pesos en runtime.

-- 1. Agregar columna posicion con default 'GENERAL'
ALTER TABLE pesos_metrica ADD COLUMN IF NOT EXISTS posicion VARCHAR(50) NOT NULL DEFAULT 'GENERAL';

-- 2. Eliminar clave primaria simple y crear clave primaria compuesta (clave, posicion)
ALTER TABLE pesos_metrica DROP CONSTRAINT IF EXISTS pesos_metrica_pkey;
ALTER TABLE pesos_metrica DROP CONSTRAINT IF EXISTS pk_pesos_metrica;
ALTER TABLE pesos_metrica ADD CONSTRAINT pk_pesos_metrica PRIMARY KEY (clave, posicion);

-- 3. Poblar las ponderaciones por posicion basadas en las metricas existentes
-- Delantero
INSERT INTO pesos_metrica (clave, posicion, peso, activo, sentido)
SELECT clave, 'Delantero', peso, activo, sentido FROM pesos_metrica WHERE posicion = 'GENERAL'
ON CONFLICT (clave, posicion) DO NOTHING;

-- Centrocampista
INSERT INTO pesos_metrica (clave, posicion, peso, activo, sentido)
SELECT clave, 'Centrocampista',
       CASE
           WHEN clave = 'goals' THEN 3.5000
           WHEN clave = 'assists' THEN 2.5000
           ELSE peso
       END,
       activo, sentido FROM pesos_metrica WHERE posicion = 'GENERAL'
ON CONFLICT (clave, posicion) DO NOTHING;

-- Defensor
INSERT INTO pesos_metrica (clave, posicion, peso, activo, sentido)
SELECT clave, 'Defensor',
       CASE
           WHEN clave = 'goals' THEN 4.5000
           WHEN clave = 'assists' THEN 2.5000
           WHEN clave = 'manOfTheMatch' THEN 3.0000
           WHEN clave = 'minsPlayed' THEN 0.4000
           ELSE peso
       END,
       activo, sentido FROM pesos_metrica WHERE posicion = 'GENERAL'
ON CONFLICT (clave, posicion) DO NOTHING;

-- Arquero
INSERT INTO pesos_metrica (clave, posicion, peso, activo, sentido)
SELECT clave, 'Arquero',
       CASE
           WHEN clave = 'goals' THEN 5.0000
           WHEN clave = 'assists' THEN 3.0000
           WHEN clave = 'manOfTheMatch' THEN 3.5000
           WHEN clave = 'minsPlayed' THEN 0.4000
           WHEN clave = 'redCard' THEN 3.5000
           ELSE peso
       END,
       activo, sentido FROM pesos_metrica WHERE posicion = 'GENERAL'
ON CONFLICT (clave, posicion) DO NOTHING;

