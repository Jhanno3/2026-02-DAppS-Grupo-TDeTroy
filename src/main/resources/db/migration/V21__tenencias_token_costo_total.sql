-- Costo de adquisicion de cada tenencia (UC-11: precio promedio de compra y ganancia/perdida del
-- portfolio). costo_total = lo que el usuario pago por los "cantidad" tokens que tiene hoy, con
-- criterio de costo promedio ponderado: cada compra suma cantidad x precio pagado y cada salida de
-- tokens descuenta la parte proporcional (ver model/TenenciaToken).
ALTER TABLE tenencias_token
ADD COLUMN costo_total NUMERIC(19, 2) NOT NULL DEFAULT 0;

-- Backfill de las tenencias existentes: cantidad actual x precio promedio de todas las compras
-- registradas del par usuario/jugador (sistema, P2P y subasta). Con costo promedio ponderado las
-- ventas no cambian el promedio, asi que el resultado es exacto salvo que haya habido compras
-- despues de una venta parcial; en ese caso es una aproximacion razonable. Una tenencia sin
-- compras registradas queda en 0 (sin ganancia/perdida calculable).
UPDATE tenencias_token t
SET costo_total = ROUND(t.cantidad * c.monto / c.cantidad, 2)
FROM (
    SELECT usuario_id, jugador_id, SUM(monto_total) AS monto, SUM(cantidad) AS cantidad
    FROM movimientos
    WHERE tipo IN ('COMPRA_SISTEMA', 'COMPRA_P2P', 'COMPRA_SUBASTA')
    GROUP BY usuario_id, jugador_id
) c
WHERE c.usuario_id = t.usuario_id
  AND c.jugador_id = t.jugador_id
  AND t.cantidad > 0
  AND c.cantidad > 0;
