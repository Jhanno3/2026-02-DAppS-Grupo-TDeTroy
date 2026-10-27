package com.tdetroy.valuacion.services;

import com.tdetroy.valuacion.entity.PesoMetricaEntity;
import com.tdetroy.valuacion.model.EstrategiaCotizacion;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Estrategia {@link EstrategiaCotizacion#RENDIMIENTO_ACTUAL}: calcula el puntaje de rendimiento en
 * base exclusivamente al desempeño absoluto de la última semana / partidos recientes ("mejores
 * jugadores del momento"), sin normalizar contra su histórico temporal.
 *
 * <p>Evalúa las métricas registradas en la semana actual frente a referencias estándar absolutas de
 * rendimiento (ej. rating WhoScored centrado en 6.0, goles, asistencias y acciones positivas). El
 * puntaje final es el promedio ponderado de esas valuaciones absolutas en el rango {@code [-1, 1]}.
 */
@Component
public class RendimientoActualCalculator implements EstrategiaCotizacionCalculator {

    private static final BigDecimal RATING_NEUTRO = new BigDecimal("6.0");
    private static final BigDecimal RATING_ESCALA = new BigDecimal("4.0");

    @Override
    public EstrategiaCotizacion getEstrategia() {
        return EstrategiaCotizacion.RENDIMIENTO_ACTUAL;
    }

    @Override
    public BigDecimal calcularPuntaje(
            Map<String, Map<String, BigDecimal>> metricasPorSemana,
            Map<String, BigDecimal> metricasSemanaActual,
            List<PesoMetricaEntity> pesosActivos) {
        if (metricasSemanaActual == null || metricasSemanaActual.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal sumaPonderada = BigDecimal.ZERO;
        BigDecimal sumaPesos = BigDecimal.ZERO;

        for (PesoMetricaEntity pesoMetrica : pesosActivos) {
            BigDecimal valorActual = metricasSemanaActual.get(pesoMetrica.getClave());
            if (valorActual == null) {
                continue;
            }

            BigDecimal puntajeMetrica = evaluarMetricaAbsoluta(pesoMetrica.getClave(), valorActual);
            sumaPonderada = sumaPonderada.add(pesoMetrica.getPeso().multiply(puntajeMetrica));
            sumaPesos = sumaPesos.add(pesoMetrica.getPeso());
        }

        if (sumaPesos.signum() == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal puntaje = sumaPonderada.divide(sumaPesos, MathContext.DECIMAL64);
        return clamp(puntaje);
    }

    /**
     * Evalúa una métrica individual contra un benchmark absoluto estándar, devolviendo un valor en
     * el rango [-1, 1].
     */
    private BigDecimal evaluarMetricaAbsoluta(String clave, BigDecimal valor) {
        String claveNormalizada = clave.toLowerCase().trim();

        if (claveNormalizada.contains("rating") || claveNormalizada.contains("calificacion")) {
            // Rating WhoScored: 6.0 es neutro (0.0), 10.0 es máximo (+1.0), 2.0 o menos es (-1.0)
            BigDecimal delta = valor.subtract(RATING_NEUTRO);
            return clamp(delta.divide(RATING_ESCALA, MathContext.DECIMAL64));
        }

        if (claveNormalizada.contains("gol") || claveNormalizada.contains("goal")) {
            // 1 gol = +0.5, 2 o más goles = +1.0
            return clamp(valor.multiply(new BigDecimal("0.5")));
        }

        if (claveNormalizada.contains("asistenc") || claveNormalizada.contains("assist")) {
            // 1 asistencia = +0.4, 2 o más asistencias = +0.8 a +1.0
            return clamp(valor.multiply(new BigDecimal("0.4")));
        }

        // Otras métricas numéricas positivas (tiros, intercepciones, pases, etc.)
        if (valor.signum() > 0) {
            // Referencia genérica estándar: escala relativa simple hasta un tope de +1.0
            BigDecimal benchmark = new BigDecimal("5.0");
            return clamp(valor.divide(benchmark, MathContext.DECIMAL64));
        }

        return BigDecimal.ZERO;
    }

    private static BigDecimal clamp(BigDecimal valor) {
        if (valor.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        }
        if (valor.compareTo(BigDecimal.ONE.negate()) < 0) {
            return BigDecimal.ONE.negate();
        }
        return valor;
    }
}
