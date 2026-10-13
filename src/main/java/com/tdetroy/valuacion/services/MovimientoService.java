package com.tdetroy.valuacion.services;

import com.tdetroy.valuacion.model.Movimiento;
import java.util.List;
import java.util.UUID;

/**
 * Historial cronológico propio de un usuario (plan.md §1, UC-12): compras/ventas contra el sistema,
 * operaciones P2P (como vendedor o comprador) y compensaciones por baja de un jugador — todas
 * quedan identificadas por su {@code TipoMovimiento} (spec.md UC-12: "identificada como tal"), este
 * Service nunca reclasifica ni filtra por tipo.
 */
public interface MovimientoService {

    /**
     * Historial completo de {@code usuarioId}, de la operación más antigua a la más reciente. Un
     * usuario sin movimientos recibe una lista vacía, nunca un error (spec.md UC-12).
     */
    List<Movimiento> obtenerHistorial(UUID usuarioId);
}
