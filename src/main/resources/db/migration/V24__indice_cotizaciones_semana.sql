-- Cotizacion de un jugador en una semana (GET /jugadores/{id}/cotizaciones/{semana}), base del
-- recalculo (semana < X) y valor inicial del ranking (DISTINCT ON ... semana): sin este indice,
-- Postgres recorre todas las cotizaciones del jugador hasta encontrar la semana.
CREATE INDEX idx_cotizaciones_historicas_jugador_semana_fecha
    ON cotizaciones_historicas (jugador_id, semana, fecha_calculo DESC);
