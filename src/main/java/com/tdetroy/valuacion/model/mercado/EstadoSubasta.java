package com.tdetroy.valuacion.model.mercado;

/**
 * {@link #ADJUDICADA}: cerró con pujas. {@link #DESIERTA}: venció sin pujas. {@link #CANCELADA}: la
 * retiró el vendedor o se dio de baja al jugador.
 */
public enum EstadoSubasta {
    ACTIVA,
    ADJUDICADA,
    DESIERTA,
    CANCELADA
}
