-- CotizacionHistorica (T4.1, plan.md S2.4): historial de cotizacion por jugador, append-only.
-- "Vigente" = ultimo registro por jugador_id segun fecha_calculo (nunca UPDATE, plan.md S2.4).
-- Sin FK a jugadores, mismo criterio que rendimientos_partido/movimientos (UUID suelto).
CREATE TABLE cotizaciones_historicas (
    id             UUID PRIMARY KEY,
    jugador_id     UUID NOT NULL,
    semana         VARCHAR(8) NOT NULL,
    valor          NUMERIC(19,2) NOT NULL,
    origen         VARCHAR(20) NOT NULL,
    fecha_calculo  TIMESTAMPTZ NOT NULL
);

-- Consulta mas probable: cotizacion vigente de un jugador (ultimo registro por fecha_calculo,
-- CotizacionServiceImpl T4.3) e historial cronologico completo (UC-08, T4.7).
CREATE INDEX idx_cotizaciones_historicas_jugador_fecha ON cotizaciones_historicas (jugador_id, fecha_calculo DESC);
