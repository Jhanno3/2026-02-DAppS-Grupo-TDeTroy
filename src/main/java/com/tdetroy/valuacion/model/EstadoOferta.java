package com.tdetroy.valuacion.model;

/**
 * Estado de una {@link OfertaP2P} (plan.md §2.7, UC-17). Toda oferta nace {@link #ABIERTA} vía
 * {@link OfertaP2P#publicar} y pasa a {@link #AGOTADA} cuando {@link OfertaP2P#ejecutarContra(int)}
 * agota {@code cantidadDisponible}, o a {@link #CANCELADA} cuando el vendedor la retira (T6.5) o el
 * jugador es dado de baja (T6.6) — en ambos casos antes de vender la totalidad.
 */
public enum EstadoOferta {
    ABIERTA,
    AGOTADA,
    CANCELADA
}
