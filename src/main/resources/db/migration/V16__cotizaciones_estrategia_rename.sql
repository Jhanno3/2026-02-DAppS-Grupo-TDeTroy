-- Renombrar la estrategia RENDIMIENTO_HISTORICO a RENDIMIENTO_TEMPORADA (mismo significado, nombre mas claro).
-- Se migran los datos ya persistidos y se actualiza el default de la columna agregada en V15.
UPDATE cotizaciones_historicas
SET estrategia = 'RENDIMIENTO_TEMPORADA'
WHERE estrategia = 'RENDIMIENTO_HISTORICO';

ALTER TABLE cotizaciones_historicas
ALTER COLUMN estrategia SET DEFAULT 'RENDIMIENTO_TEMPORADA';
