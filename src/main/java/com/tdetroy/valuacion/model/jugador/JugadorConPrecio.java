package com.tdetroy.valuacion.model.jugador;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Jugador con su cotización vigente, de sólo lectura.
 *
 * @param precio {@code null} si nunca tuvo cotización
 * @param variacionPorcentual variación desde el primer cálculo; {@code null} si tiene menos de dos,
 *     {@code 0} si no varió
 */
public record JugadorConPrecio(Jugador jugador, BigDecimal precio, BigDecimal variacionPorcentual) {

    /**
     * Las cotizaciones son {@code null} si el jugador nunca tuvo una. No hay variación si vigente e
     * inicial son el mismo cálculo (misma fecha).
     */
    public static JugadorConPrecio desdeCotizaciones(
            Jugador jugador,
            BigDecimal valorVigente,
            Instant fechaVigente,
            BigDecimal valorInicial,
            Instant fechaInicial) {
        return new JugadorConPrecio(
                jugador,
                valorVigente,
                variacion(valorVigente, fechaVigente, valorInicial, fechaInicial));
    }

    private static BigDecimal variacion(
            BigDecimal valorVigente,
            Instant fechaVigente,
            BigDecimal valorInicial,
            Instant fechaInicial) {
        if (valorVigente == null
                || valorInicial == null
                || fechaVigente.equals(fechaInicial)
                || valorInicial.signum() == 0) {
            return null;
        }
        return valorVigente
                .subtract(valorInicial)
                .divide(valorInicial, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}
