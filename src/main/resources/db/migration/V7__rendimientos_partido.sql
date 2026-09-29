-- RendimientoPartido (T3.1, plan.md S2.3): insumo de ingesta semanal (UC-05), append-only.
-- metricas queda como JSONB sin columnas fijas -- WhoScored (T3.3, todavia sin implementar)
-- puede agregar/quitar campos sin requerir una migracion de esquema (plan.md S2.3).
-- partido_externo_id + jugador_id es la clave de deduplicacion frente a una reingesta del mismo
-- ciclo semanal. Sin FK a jugadores, mismo criterio que movimientos/tenencias_token (UUID suelto).
CREATE TABLE rendimientos_partido (
    id                  UUID PRIMARY KEY,
    jugador_id          UUID NOT NULL,
    partido_externo_id  VARCHAR(100) NOT NULL,
    fecha_partido       DATE NOT NULL,
    semana_calculo      VARCHAR(8) NOT NULL,
    metricas            JSONB NOT NULL,
    fuente_resultado    VARCHAR(20) NOT NULL,
    fecha_ingesta       TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_rendimientos_partido_partido_jugador UNIQUE (partido_externo_id, jugador_id)
);

-- Consulta mas probable prevista: rendimiento de un jugador en la semana de calculo vigente
-- (CotizacionService, plan.md S6.1, T4.3).
CREATE INDEX idx_rendimientos_partido_jugador_semana ON rendimientos_partido (jugador_id, semana_calculo);
