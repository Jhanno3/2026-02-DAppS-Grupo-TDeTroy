package com.tdetroy.valuacion.model;

import com.tdetroy.valuacion.common.Monetario;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;

/**
 * Variación semanal de la cotización de un jugador (plan.md §6.1-§6.3): concentra las reglas de
 * negocio del recálculo, sin acceso a datos — {@code CotizacionServiceImpl} sólo junta las
 * métricas, la cotización anterior y los parámetros configurados, y aplica el resultado.
 *
 * <p>{@link #getFactor()} es la variación porcentual a aplicar sobre la cotización anterior ({@code
 * 0.05} = +5%), siempre dentro de {@code [-VARIACION_MAXIMA, +VARIACION_MAXIMA]}.
 */
@Getter
public final class AjusteCotizacion {

    /**
     * Tope de variación por recálculo, en ambos sentidos (plan.md §6.1: "evita saltos irreales").
     */
    public static final BigDecimal VARIACION_MAXIMA = new BigDecimal("0.20");

    private final BigDecimal factor;

    private AjusteCotizacion(BigDecimal factor) {
        this.factor = factor;
    }

    /**
     * Calcula el ajuste de la semana con la estrategia indicada:
     *
     * <ul>
     *   <li>sin métricas en la semana (no jugó o no hubo ingesta): baja fija por inactividad;
     *   <li>con métricas: {@code puntaje × sensibilidad}, acotado a {@link #VARIACION_MAXIMA}.
     * </ul>
     *
     * @param metricasPorSemana métricas del histórico reciente del jugador, sumadas por semana
     * @param metricasSemanaActual métricas sumadas de la semana que se cotiza
     * @param pesosActivos pesos configurados y activos del motor de cotización
     * @param sensibilidad multiplicador del puntaje ({@code app.cotizacion.sensibilidad})
     * @param decayInactividad baja por semana sin datos ({@code app.cotizacion.decay-inactividad})
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

    /**
     * Ajuste por rendimiento: {@code puntaje × sensibilidad}, acotado a ±{@link #VARIACION_MAXIMA}.
     */
    public static AjusteCotizacion porRendimiento(BigDecimal puntaje, BigDecimal sensibilidad) {
        Objects.requireNonNull(puntaje, "puntaje no puede ser null");
        Objects.requireNonNull(sensibilidad, "sensibilidad no puede ser null");
        return new AjusteCotizacion(acotar(puntaje.multiply(sensibilidad)));
    }

    /** Ajuste por inactividad: baja fija de {@code decayInactividad} (ej. {@code 0.01} = -1%). */
    public static AjusteCotizacion porInactividad(BigDecimal decayInactividad) {
        Objects.requireNonNull(decayInactividad, "decayInactividad no puede ser null");
        if (decayInactividad.signum() < 0) {
            throw new IllegalArgumentException(
                    "decayInactividad no puede ser negativo, fue " + decayInactividad);
        }
        return new AjusteCotizacion(acotar(decayInactividad.negate()));
    }

    /**
     * Cotización resultante de aplicar este ajuste: {@code anterior × (1 + factor)}, redondeada a
     * la escala monetaria del sistema.
     */
    public BigDecimal aplicarA(BigDecimal cotizacionAnterior) {
        Objects.requireNonNull(cotizacionAnterior, "cotizacionAnterior no puede ser null");
        return Monetario.escalar(cotizacionAnterior.multiply(BigDecimal.ONE.add(factor)));
    }

    private static BigDecimal acotar(BigDecimal factor) {
        return factor.max(VARIACION_MAXIMA.negate()).min(VARIACION_MAXIMA);
    }
}
