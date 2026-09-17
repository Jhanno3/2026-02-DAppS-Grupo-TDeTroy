package com.tdetroy.valuacion.model;

/**
 * Tipo de un {@link Movimiento} (plan.md §2.6).
 *
 * <p>{@link #RECARGA_SALDO} es el único tipo sin jugador/cantidad/precio asociado. Sólo
 * {@link #COMPRA_P2P} y {@link #VENTA_P2P} llevan {@code contraparteUsuarioId} (el otro
 * usuario de la operación).
 */
public enum TipoMovimiento {
    COMPRA_SISTEMA,
    VENTA_SISTEMA,
    COMPRA_P2P,
    VENTA_P2P,
    RECARGA_SALDO,
    COMPENSACION_BAJA
}
