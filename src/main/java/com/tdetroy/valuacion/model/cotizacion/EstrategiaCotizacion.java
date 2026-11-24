package com.tdetroy.valuacion.model.cotizacion;

import com.tdetroy.valuacion.model.rendimiento.CalculadorRendimientoActual;
import com.tdetroy.valuacion.model.rendimiento.CalculadorRendimientoTemporada;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Estrategias de cálculo de cotización, cada una con su {@link CalculadorEstrategiaCotizacion}.
 *
 * <ul>
 *   <li>{@link #RENDIMIENTO_TEMPORADA}: compara al jugador contra su propio histórico.
 *   <li>{@link #RENDIMIENTO_ACTUAL}: mide el rendimiento absoluto de la última semana.
 * </ul>
 */
public enum EstrategiaCotizacion {
    RENDIMIENTO_TEMPORADA(new CalculadorRendimientoTemporada()),
    RENDIMIENTO_ACTUAL(new CalculadorRendimientoActual());

    private final CalculadorEstrategiaCotizacion calculador;

    EstrategiaCotizacion(CalculadorEstrategiaCotizacion calculador) {
        this.calculador = calculador;
    }

    public BigDecimal calcularPuntaje(
            Map<String, Map<String, BigDecimal>> metricasPorSemana,
            Map<String, BigDecimal> metricasSemanaActual,
            List<PesoMetrica> pesosActivos) {
        return calculador.calcularPuntaje(metricasPorSemana, metricasSemanaActual, pesosActivos);
    }
}
