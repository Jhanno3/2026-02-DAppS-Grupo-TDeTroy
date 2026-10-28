package com.tdetroy.valuacion.services;

import com.tdetroy.valuacion.entity.PesoMetricaEntity;
import com.tdetroy.valuacion.model.EstrategiaCotizacion;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Estrategia de cálculo del puntaje de rendimiento para la valuación de jugadores (UC-06). */
public interface EstrategiaCotizacionCalculator {

    /** Identificador de la estrategia. */
    EstrategiaCotizacion getEstrategia();

    /**
     * Calcula el puntaje de rendimiento en el rango [-1, 1] a partir de las métricas disponibles.
     *
     * @param metricasPorSemana métricas agrupadas por semana del histórico reciente del jugador
     * @param metricasSemanaActual métricas sumadas de la semana en curso
     * @param pesosActivos lista de pesos configurados y activos en el sistema
     * @return puntaje de rendimiento normalizado en [-1, 1], donde 0 es neutro
     */
    BigDecimal calcularPuntaje(
            Map<String, Map<String, BigDecimal>> metricasPorSemana,
            Map<String, BigDecimal> metricasSemanaActual,
            List<PesoMetricaEntity> pesosActivos);
}
