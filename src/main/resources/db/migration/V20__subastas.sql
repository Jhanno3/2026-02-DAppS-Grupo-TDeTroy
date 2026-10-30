-- Subasta (UC-18): subasta de tokens entre usuarios. precio_actual/mejor_postor_id son NULL hasta
-- la primera puja. fecha_vencimiento es el plazo para que llegue la primera puja; fecha_cierre
-- (NULL sin pujas) es ultima puja + 2 minutos -- la subasta se adjudica al pasar fecha_cierre sin
-- pujas nuevas. Estado/precio nunca se actualizan por SQL directo fuera de la app -- siempre via
-- model/Subasta (constitution.md S2). Sin FK a usuarios/jugadores, mismo criterio que ofertas_p2p.
CREATE TABLE subastas (
    id                 UUID PRIMARY KEY,
    vendedor_id        UUID NOT NULL,
    jugador_id         UUID NOT NULL,
    cantidad           INTEGER NOT NULL,
    precio_inicial     NUMERIC(19, 2) NOT NULL,
    precio_actual      NUMERIC(19, 2),
    mejor_postor_id    UUID,
    cantidad_pujas     INTEGER NOT NULL,
    estado             VARCHAR(12) NOT NULL,
    fecha_creacion     TIMESTAMPTZ NOT NULL,
    fecha_vencimiento  TIMESTAMPTZ NOT NULL,
    fecha_cierre       TIMESTAMPTZ
);

-- Consultas mas frecuentes previstas: listado de subastas activas (global y por jugador) y el
-- chequeo periodico de cierre (SubastaCierreScheduler) sobre las ACTIVA.
CREATE INDEX idx_subastas_estado ON subastas (estado, fecha_creacion DESC);
CREATE INDEX idx_subastas_jugador_estado ON subastas (jugador_id, estado);

-- Historial append-only de pujas de una subasta (UC-18), para mostrar la evolucion del precio.
-- No se usa para calcular nada: el precio vigente vive en subastas.precio_actual.
CREATE TABLE pujas_subasta (
    id               UUID PRIMARY KEY,
    subasta_id       UUID NOT NULL,
    postor_id        UUID NOT NULL,
    precio_unitario  NUMERIC(19, 2) NOT NULL,
    fecha            TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_pujas_subasta_subasta_fecha ON pujas_subasta (subasta_id, fecha);
