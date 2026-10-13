package com.tdetroy.valuacion.dto.response;

import com.tdetroy.valuacion.model.Movimiento;
import com.tdetroy.valuacion.model.TipoMovimiento;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Respuesta de {@code POST /jugadores/{id}/compras}/{@code .../ventas} (UC-09/UC-10) y de un
 * elemento de {@code GET /movimientos/me} (UC-12) — un registro de {@link Movimiento}. {@code tipo}
 * ya identifica si es compra/venta contra el sistema, una operación P2P o una compensación por baja
 * (spec.md UC-12: "identificada como tal, no como una venta/compra ordinaria").
 */
public record MovimientoResponse(
        UUID id,
        UUID jugadorId,
        TipoMovimiento tipo,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal montoTotal,
        UUID contraparteUsuarioId,
        Instant fecha) {

    public static MovimientoResponse desde(Movimiento movimiento) {
        return new MovimientoResponse(
                movimiento.getId(),
                movimiento.getJugadorId(),
                movimiento.getTipo(),
                movimiento.getCantidad(),
                movimiento.getPrecioUnitario(),
                movimiento.getMontoTotal(),
                movimiento.getContraparteUsuarioId(),
                movimiento.getFecha());
    }
}
