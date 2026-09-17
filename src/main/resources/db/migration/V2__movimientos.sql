-- Movimiento (T0.6, plan.md §2.6): registro append-only de cara al usuario. Building block
-- compartido, creado antes que usuarios/jugadores (Fase 0) -- por eso usuario_id/jugador_id/
-- contraparte_usuario_id son UUID sueltos, sin FK todavia. Cuando existan las tablas
-- usuarios/jugadores (Fase 1/2) se puede evaluar agregar la FK en una migracion aparte.
CREATE TABLE movimientos (
    id                     UUID PRIMARY KEY,
    usuario_id             UUID NOT NULL,
    jugador_id             UUID,
    tipo                   VARCHAR(30) NOT NULL,
    cantidad               INTEGER,
    precio_unitario        NUMERIC(19, 2),
    monto_total            NUMERIC(19, 2) NOT NULL,
    contraparte_usuario_id UUID,
    fecha                  TIMESTAMPTZ NOT NULL
);

-- Consulta mas frecuente prevista (UC-12, MovimientoService en T5.5): historial propio
-- cronologico de un usuario.
CREATE INDEX idx_movimientos_usuario_id ON movimientos (usuario_id, fecha DESC);
