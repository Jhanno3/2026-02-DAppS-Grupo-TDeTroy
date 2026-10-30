package com.tdetroy.valuacion.model;

/**
 * Tipo de un {@link Movimiento} (plan.md §2.6).
 *
 * <p>{@link #RECARGA_SALDO} es el único tipo sin jugador/cantidad/precio asociado. Sólo {@link
 * #COMPRA_P2P}, {@link #VENTA_P2P}, {@link #COMPRA_SUBASTA} y {@link #VENTA_SUBASTA} llevan {@code
 * contraparteUsuarioId} (el otro usuario de la operación).
 */
public enum TipoMovimiento {
    COMPRA_SISTEMA,
    VENTA_SISTEMA,
    COMPRA_P2P,
    VENTA_P2P,
    COMPRA_SUBASTA,
    VENTA_SUBASTA,
    RECARGA_SALDO,
    COMPENSACION_BAJA
}
