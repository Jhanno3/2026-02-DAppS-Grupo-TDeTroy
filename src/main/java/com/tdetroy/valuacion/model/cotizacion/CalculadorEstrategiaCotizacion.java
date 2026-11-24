package com.tdetroy.valuacion.model.cotizacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface CalculadorEstrategiaCotizacion {

    EstrategiaCotizacion getEstrategia();

    /**
     * @param metricasPorSemana histórico reciente del jugador, sumado por semana
     * @param metricasSemanaActual métricas sumadas de la semana en curso
     * @return puntaje en {@code [-1, 1]}, donde 0 es neutro
     */
    BigDecimal calcularPuntaje(
            Map<String, Map<String, BigDecimal>> metricasPorSemana,
            Map<String, BigDecimal> metricasSemanaActual,
            List<PesoMetrica> pesosActivos);
}
