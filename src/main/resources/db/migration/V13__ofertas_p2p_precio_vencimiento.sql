-- OfertaP2P.precioUnitario/fechaVencimiento (UC-17, rediseno de la via P2P): el vendedor fija su
-- propio precio por token al publicar la oferta (fijo durante toda su vida, nunca recalculado
-- contra la cotizacion vigente) y un limite de horas -- si nadie la compra (del todo o en parte)
-- antes de esa fecha, el chequeo periodico de OfertaServiceImpl.cancelarVencidas() la cancela
-- automaticamente y reintegra lo no vendido a la tenencia del vendedor, mismo mecanismo que una
-- cancelacion voluntaria. Default transitorio sobre filas ya persistidas (mismo criterio que
-- V11__jugadores_liga.sql): toda escritura nueva exige ambos valores explicitamente.
ALTER TABLE ofertas_p2p ADD COLUMN precio_unitario NUMERIC(19,2) NOT NULL DEFAULT 0;
ALTER TABLE ofertas_p2p ALTER COLUMN precio_unitario DROP DEFAULT;
ALTER TABLE ofertas_p2p ADD COLUMN fecha_vencimiento TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE ofertas_p2p ALTER COLUMN fecha_vencimiento DROP DEFAULT;

-- Consulta mas frecuente prevista: el chequeo periodico de ofertas ABIERTA ya vencidas
-- (OfertaVencimientoScheduler).
CREATE INDEX idx_ofertas_p2p_estado_vencimiento ON ofertas_p2p (estado, fecha_vencimiento);
