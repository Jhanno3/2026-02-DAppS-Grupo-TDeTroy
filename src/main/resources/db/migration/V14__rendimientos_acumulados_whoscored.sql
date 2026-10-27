-- RendimientoAcumuladoWhoScored (plan.md S8.1): snapshot 1:1 por jugador del ultimo acumulado de
-- temporada conocido desde la pagina publica de liga de WhoScored. A diferencia de
-- rendimientos_partido, NO es append-only: cada ingesta semanal sobrescribe el snapshot completo
-- del jugador (jugador_id es la PK). El delta contra el valor anterior (actual - anterior, nunca
-- negativo) es lo que efectivamente se persiste como una fila de rendimientos_partido (ver
-- fuente_resultado WHOSCORED_LIGA) -- esta tabla solo guarda el ultimo acumulado, no historial.
-- Sin FK a jugadores, mismo criterio que rendimientos_partido/movimientos/tenencias_token.
CREATE TABLE rendimientos_acumulados_whoscored (
    jugador_id            UUID PRIMARY KEY,
    metricas              JSONB NOT NULL,
    fecha_actualizacion   TIMESTAMPTZ NOT NULL
);
