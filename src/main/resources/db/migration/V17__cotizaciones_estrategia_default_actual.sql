-- La estrategia por defecto de la app pasa de RENDIMIENTO_TEMPORADA a RENDIMIENTO_ACTUAL.
-- No se migran datos: las cotizaciones ya persistidas conservan la estrategia con la que se calcularon.
ALTER TABLE cotizaciones_historicas
ALTER COLUMN estrategia SET DEFAULT 'RENDIMIENTO_ACTUAL';
