-- Ganancia realizada de cada tenencia (UC-11): acumula, por cada salida de tokens, lo cobrado menos
-- el costo de lo que salio (ver model/TenenciaToken). Sobrevive a la tenencia: sigue contando en el
-- portfolio aunque cantidad llegue a cero.
ALTER TABLE tenencias_token
ADD COLUMN ganancia_realizada NUMERIC(19, 2) NOT NULL DEFAULT 0;

-- Backfill: las salidas anteriores no guardaron su costo, asi que se estima con el precio promedio
-- de todas las compras del par usuario/jugador (mismo criterio y misma aproximacion que V21). Un
-- par con salidas pero sin compras registradas queda en 0.
UPDATE tenencias_token t
SET ganancia_realizada = ROUND(v.monto - v.cantidad * c.monto / c.cantidad, 2)
FROM (
    SELECT usuario_id, jugador_id, SUM(monto_total) AS monto, SUM(cantidad) AS cantidad
    FROM movimientos
    WHERE tipo IN ('COMPRA_SISTEMA', 'COMPRA_P2P', 'COMPRA_SUBASTA')
    GROUP BY usuario_id, jugador_id
) c
JOIN (
    SELECT usuario_id, jugador_id, SUM(monto_total) AS monto, SUM(cantidad) AS cantidad
    FROM movimientos
    WHERE tipo IN ('VENTA_SISTEMA', 'VENTA_P2P', 'VENTA_SUBASTA', 'COMPENSACION_BAJA')
    GROUP BY usuario_id, jugador_id
) v ON v.usuario_id = c.usuario_id AND v.jugador_id = c.jugador_id
WHERE c.usuario_id = t.usuario_id
  AND c.jugador_id = t.jugador_id
  AND c.cantidad > 0;
