package com.tdetroy.valuacion.model.cartera;

/** Sólo {@link #RECARGA_SALDO} no lleva jugador; sólo P2P y subasta llevan contraparte. */
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
