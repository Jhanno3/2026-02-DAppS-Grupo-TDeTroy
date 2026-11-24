package com.tdetroy.valuacion.model.jugador;

import java.math.BigDecimal;

/**
 * Jugador con su cotización vigente, de sólo lectura.
 *
 * @param precio {@code null} si nunca tuvo cotización
 * @param variacionPorcentual variación desde el primer cálculo; {@code null} si tiene menos de dos,
 *     {@code 0} si no varió
 */
public record JugadorConPrecio(
        Jugador jugador, BigDecimal precio, BigDecimal variacionPorcentual) {}
