package com.tdetroy.valuacion.model.rendimiento;

import com.tdetroy.valuacion.model.cotizacion.CalculadorEstrategiaCotizacion;
import com.tdetroy.valuacion.model.cotizacion.EstrategiaCotizacion;
import com.tdetroy.valuacion.model.cotizacion.PesoMetrica;
import com.tdetroy.valuacion.model.cotizacion.SentidoMetrica;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Map;

/**
 * Promedio ponderado de las métricas de la semana contra referencias absolutas, sin mirar el
 * histórico. Las de sentido {@link SentidoMetrica#NEGATIVO} se invierten.
 */
public class CalculadorRendimientoActual implements CalculadorEstrategiaCotizacion {

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
            List<PesoMetrica> pesosActivos) {
        if (metricasSemanaActual == null || metricasSemanaActual.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Promedio ponderado: sum(peso * puntajeMetrica) / sum(peso).
        BigDecimal sumaPonderada = BigDecimal.ZERO;
        BigDecimal sumaPesos = BigDecimal.ZERO;

        for (PesoMetrica pesoMetrica : pesosActivos) {
            // Una métrica con peso pero sin dato esta semana no participa (ni suma su peso).
            BigDecimal valorActual = metricasSemanaActual.get(pesoMetrica.getClave());
            if (valorActual == null) {
                continue;
            }

            // Valor crudo -> puntaje en [-1, 1] contra una referencia fija; las métricas de
            // sentido NEGATIVO (tarjetas) se invierten para que resten.
            BigDecimal puntajeMetrica = evaluarMetricaAbsoluta(pesoMetrica.getClave(), valorActual);
            BigDecimal puntajeConSentido =
                    pesoMetrica.getSentido() == SentidoMetrica.NEGATIVO
                            ? puntajeMetrica.negate()
                            : puntajeMetrica;
            sumaPonderada = sumaPonderada.add(pesoMetrica.getPeso().multiply(puntajeConSentido));
            sumaPesos = sumaPesos.add(pesoMetrica.getPeso());
        }

        if (sumaPesos.signum() == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal puntaje = sumaPonderada.divide(sumaPesos, MathContext.DECIMAL64);
        return acotar(puntaje);
    }

    private BigDecimal evaluarMetricaAbsoluta(String clave, BigDecimal valor) {
        String claveNormalizada = clave.toLowerCase().trim();

        if (claveNormalizada.contains("rating") || claveNormalizada.contains("calificacion")) {
            // Rating WhoScored: 6.0 es neutro (0.0), 10.0 es máximo (+1.0), 2.0 o menos es (-1.0)
            BigDecimal delta = valor.subtract(RATING_NEUTRO);
            return acotar(delta.divide(RATING_ESCALA, MathContext.DECIMAL64));
        }

        if (claveNormalizada.contains("gol") || claveNormalizada.contains("goal")) {
            // 1 gol = +0.5, 2 o más goles = +1.0
            return acotar(valor.multiply(new BigDecimal("0.5")));
        }

        if (claveNormalizada.contains("asistenc") || claveNormalizada.contains("assist")) {
            // 1 asistencia = +0.4, 2 o más asistencias = +0.8 a +1.0
            return acotar(valor.multiply(new BigDecimal("0.4")));
        }

        // Otras métricas numéricas positivas (tiros, intercepciones, pases, etc.)
        if (valor.signum() > 0) {
            // Referencia genérica estándar: escala relativa simple hasta un tope de +1.0
            BigDecimal referencia = new BigDecimal("5.0");
            return acotar(valor.divide(referencia, MathContext.DECIMAL64));
        }

        return BigDecimal.ZERO;
    }

    private static BigDecimal acotar(BigDecimal valor) {
        if (valor.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        }
        if (valor.compareTo(BigDecimal.ONE.negate()) < 0) {
            return BigDecimal.ONE.negate();
        }
        return valor;
    }
}
