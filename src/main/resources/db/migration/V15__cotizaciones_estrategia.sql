-- Agregar columna estrategia a cotizaciones_historicas para soportar multiples metodos/estrategias de valuacion (UC-06).
-- Default 'RENDIMIENTO_HISTORICO' para compatibilidad hacia atras con todas las cotizaciones ya persistidas.
ALTER TABLE cotizaciones_historicas
ADD COLUMN estrategia VARCHAR(30) NOT NULL DEFAULT 'RENDIMIENTO_HISTORICO';

-- Indice para consultar eficientemente la cotizacion vigente e historial por jugador y estrategia
CREATE INDEX idx_cotizaciones_historicas_jugador_estrategia_fecha
ON cotizaciones_historicas (jugador_id, estrategia, fecha_calculo DESC);
