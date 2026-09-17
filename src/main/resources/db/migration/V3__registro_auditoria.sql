-- RegistroAuditoria (T0.5, plan.md §2.8/§10): log de auditoria interno, append-only,
-- sin endpoint publico en el MVP. actor_id nulo = SISTEMA (job semanal, etc).
-- valores_antes/valores_despues quedan tal cual los serializa AuditoriaService (services/),
-- nunca la propia entidad -- ver comentario en model/RegistroAuditoria.java.
CREATE TABLE registros_auditoria (
    id                UUID PRIMARY KEY,
    actor_id          UUID,
    accion            VARCHAR(100) NOT NULL,
    entidad_afectada  VARCHAR(100) NOT NULL,
    entidad_id        UUID NOT NULL,
    valores_antes     JSONB,
    valores_despues   JSONB NOT NULL,
    fecha             TIMESTAMPTZ NOT NULL
);

-- Consulta mas probable para trazabilidad interna: historial de una entidad puntual.
CREATE INDEX idx_registros_auditoria_entidad ON registros_auditoria (entidad_afectada, entidad_id, fecha DESC);
