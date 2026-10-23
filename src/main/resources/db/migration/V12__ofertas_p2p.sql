-- OfertaP2P (T6.1, plan.md S2.7, S9, UC-17): oferta abierta de venta directa entre usuarios.
-- cantidad_disponible/estado nunca se actualizan por SQL directo fuera de la app -- siempre via
-- OfertaP2P.ejecutarContra (constitution.md S2, invariante "nunca se vende mas cantidad que la
-- publicada"). Sin FK a usuarios/jugadores, mismo criterio que tenencias_token/movimientos (UUID
-- sueltos, sin acoplar el esquema entre aggregates).
CREATE TABLE ofertas_p2p (
    id                   UUID PRIMARY KEY,
    vendedor_id          UUID NOT NULL,
    jugador_id           UUID NOT NULL,
    cantidad_publicada   INTEGER NOT NULL,
    cantidad_disponible  INTEGER NOT NULL,
    estado               VARCHAR(10) NOT NULL,
    fecha_creacion       TIMESTAMPTZ NOT NULL
);

-- Consulta mas frecuente prevista: listado de ofertas abiertas de un jugador para que un
-- comprador elija (GET /ofertas?jugadorId={id}, UC-17, T6.3).
CREATE INDEX idx_ofertas_p2p_jugador_estado ON ofertas_p2p (jugador_id, estado);
