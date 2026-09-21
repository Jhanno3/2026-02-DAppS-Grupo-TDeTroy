-- TenenciaToken (T2.2, plan.md §2.5): base de lectura del portfolio, no existe tabla Portfolio
-- separada. cantidad/cantidad_reservada nunca se actualizan por SQL directo fuera de la app --
-- siempre via TenenciaToken.acreditar/debitar/reservar/liberarReserva/ejecutarReserva
-- (constitution.md S2, invariante cantidad_reservada <= cantidad). Sin FK a usuarios/jugadores,
-- mismo criterio que movimientos/registros_auditoria (UUID sueltos, sin acoplar el esquema entre
-- aggregates).
CREATE TABLE tenencias_token (
    id                  UUID PRIMARY KEY,
    usuario_id          UUID NOT NULL,
    jugador_id          UUID NOT NULL,
    cantidad            INTEGER NOT NULL,
    cantidad_reservada  INTEGER NOT NULL,
    CONSTRAINT uk_tenencias_token_usuario_jugador UNIQUE (usuario_id, jugador_id)
);

-- Consulta mas frecuente prevista: portfolio de un usuario (UC-11, PortfolioService en T5.3).
CREATE INDEX idx_tenencias_token_usuario_id ON tenencias_token (usuario_id);
