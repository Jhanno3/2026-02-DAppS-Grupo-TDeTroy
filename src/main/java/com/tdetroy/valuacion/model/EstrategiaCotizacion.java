package com.tdetroy.valuacion.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Estrategias / métodos de cálculo de cotización disponibles (UC-06).
 *
 * <ul>
 *   <li>{@link #RENDIMIENTO_TEMPORADA}: valúa al jugador comparando su rendimiento reciente contra
 *       su propio histórico temporal (normalización min-max de métricas a lo largo de las semanas).
 *       Mide consistencia y evolución respecto a sí mismo.
 *   <li>{@link #RENDIMIENTO_ACTUAL}: valúa al jugador en base a su rendimiento absoluto en la
 *       última semana / partidos recientes (calificación y acciones destacadas inmediatas). Mide
 *       impacto actual y momentum ("mejores jugadores del momento").
 * </ul>
 *
 * <p>Cada estrategia conoce su propio {@link EstrategiaCotizacionCalculator} (Strategy pattern):
 * quien cotiza sólo elige la estrategia y le pide el puntaje, sin saber qué algoritmo hay detrás.
 */
public enum EstrategiaCotizacion {
    RENDIMIENTO_TEMPORADA(new RendimientoTemporadaCalculator()),
    RENDIMIENTO_ACTUAL(new RendimientoActualCalculator());

    private final EstrategiaCotizacionCalculator calculador;

    EstrategiaCotizacion(EstrategiaCotizacionCalculator calculador) {
        this.calculador = calculador;
    }

    /**
     * Puntaje de rendimiento en {@code [-1, 1]} según el algoritmo de esta estrategia.
     *
     * @see EstrategiaCotizacionCalculator#calcularPuntaje
     */
    public BigDecimal calcularPuntaje(
            Map<String, Map<String, BigDecimal>> metricasPorSemana,
            Map<String, BigDecimal> metricasSemanaActual,
            List<PesoMetrica> pesosActivos) {
        return calculador.calcularPuntaje(metricasPorSemana, metricasSemanaActual, pesosActivos);
    }
}
