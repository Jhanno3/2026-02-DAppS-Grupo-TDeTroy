package com.tdetroy.valuacion.model.cotizacion;

import com.tdetroy.valuacion.common.Monetario;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;

/**
 * Variación semanal de la cotización. {@code factor} es la variación sobre la anterior ({@code
 * 0.05} = +5%).
 */
@Getter
public final class AjusteCotizacion {

    /** Tope por recálculo, en ambos sentidos, para evitar saltos irreales. */
    public static final BigDecimal VARIACION_MAXIMA = new BigDecimal("0.20");

    private final BigDecimal factor;

    private AjusteCotizacion(BigDecimal factor) {
        this.factor = factor;
    }

    /**
     * Sin métricas en la semana aplica la baja por inactividad; con métricas, ajusta por
     * rendimiento.
     *
     * @param metricasPorSemana histórico reciente del jugador, sumado por semana
     * @param metricasSemanaActual métricas sumadas de la semana que se cotiza
     * @param sensibilidad multiplicador del puntaje
     */
    public static AjusteCotizacion calcular(
            EstrategiaCotizacion estrategia,
            Map<String, Map<String, BigDecimal>> metricasPorSemana,
            Map<String, BigDecimal> metricasSemanaActual,
            List<PesoMetrica> pesosActivos,
            BigDecimal sensibilidad,
            BigDecimal decayInactividad) {
        Objects.requireNonNull(estrategia, "estrategia no puede ser null");
        Objects.requireNonNull(metricasSemanaActual, "metricasSemanaActual no puede ser null");
        if (metricasSemanaActual.isEmpty()) {
            return porInactividad(decayInactividad);
        }
        BigDecimal puntaje =
                estrategia.calcularPuntaje(metricasPorSemana, metricasSemanaActual, pesosActivos);
        return porRendimiento(puntaje, sensibilidad);
    }

    public static AjusteCotizacion porRendimiento(BigDecimal puntaje, BigDecimal sensibilidad) {
        Objects.requireNonNull(puntaje, "puntaje no puede ser null");
        Objects.requireNonNull(sensibilidad, "sensibilidad no puede ser null");
        return new AjusteCotizacion(acotar(puntaje.multiply(sensibilidad)));
    }

    public static AjusteCotizacion porInactividad(BigDecimal decayInactividad) {
        Objects.requireNonNull(decayInactividad, "decayInactividad no puede ser null");
        if (decayInactividad.signum() < 0) {
            throw new IllegalArgumentException(
                    "decayInactividad no puede ser negativo, fue " + decayInactividad);
        }
        return new AjusteCotizacion(acotar(decayInactividad.negate()));
    }

    public BigDecimal aplicarA(BigDecimal cotizacionAnterior) {
        Objects.requireNonNull(cotizacionAnterior, "cotizacionAnterior no puede ser null");
        return Monetario.escalar(cotizacionAnterior.multiply(BigDecimal.ONE.add(factor)));
    }

    private static BigDecimal acotar(BigDecimal factor) {
        return factor.max(VARIACION_MAXIMA.negate()).min(VARIACION_MAXIMA);
    }
}
