package com.tdetroy.valuacion.model;

/**
 * Estado de una {@link Subasta} (UC-18). Toda subasta nace {@link #ACTIVA}; pasa a {@link
 * #ADJUDICADA} cuando pasan 2 minutos sin pujas desde la última, a {@link #DESIERTA} cuando vence
 * la duración sin ninguna puja, o a {@link #CANCELADA} cuando el vendedor la retira o el jugador es
 * dado de baja.
 */
public enum EstadoSubasta {
    ACTIVA,
    ADJUDICADA,
    DESIERTA,
    CANCELADA
}
