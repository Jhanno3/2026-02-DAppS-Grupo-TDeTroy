-- Jugador (T2.1, plan.md §2.2): catalogo de jugadores tokenizables. tokens_emitidos nunca se
-- actualiza por SQL directo fuera de la app -- siempre via Jugador.emitirTokens/liberarTokens
-- (constitution.md S2, invariante de maximo 100). cotizacion_vigente_id/
-- fecha_ultima_actualizacion_rendimiento quedan NULL hasta que Cotizacion (Fase 4) y
-- Rendimiento (Fase 3) existan -- sin FK todavia, mismo criterio que movimientos
-- (V2__movimientos.sql): son UUID sueltos hasta que la tabla referenciada exista.
CREATE TABLE jugadores (
    id                                      UUID PRIMARY KEY,
    nombre                                  VARCHAR(255) NOT NULL,
    club                                    VARCHAR(255) NOT NULL,
    posicion                                VARCHAR(100) NOT NULL,
    fecha_nacimiento                        DATE NOT NULL,
    nacionalidad                            VARCHAR(100) NOT NULL,
    estado                                  VARCHAR(10) NOT NULL,
    tokens_emitidos                         INTEGER NOT NULL,
    cotizacion_vigente_id                   UUID,
    fecha_ultima_actualizacion_rendimiento  TIMESTAMPTZ
);

-- Consultas mas frecuentes previstas: catalogo publico (UC-07) y el job semanal de recotizacion
-- (plan.md S6.4, T4.5), que corre unicamente sobre jugadores ACTIVO.
CREATE INDEX idx_jugadores_estado ON jugadores (estado);
