package com.tdetroy.valuacion.model;

/**
 * Estado de un {@link Jugador} (plan.md §2.2). Todo jugador nace {@link #ACTIVO} vía {@link
 * Jugador#darAlta} y pasa a {@link #INACTIVO} exclusivamente a través de {@link
 * Jugador#darDeBaja()} (UC-15) — no hay baja física del catálogo.
 */
public enum EstadoJugador {
    ACTIVO,
    INACTIVO
}
