package com.tdetroy.valuacion.model.rendimiento;

import com.tdetroy.valuacion.model.cotizacion.CalculadorEstrategiaCotizacion;
import com.tdetroy.valuacion.model.cotizacion.EstrategiaCotizacion;
import com.tdetroy.valuacion.model.cotizacion.PesoMetrica;
import com.tdetroy.valuacion.model.cotizacion.SentidoMetrica;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Promedio ponderado de cada métrica normalizada min-max a {@code [-1, 1]} sobre el histórico del
 * jugador; con menos de dos semanas o rango cero aporta 0. Las de sentido {@link
 * SentidoMetrica#NEGATIVO} se invierten.
 */
public class CalculadorRendimientoTemporada implements CalculadorEstrategiaCotizacion {

    private static final BigDecimal DOS = BigDecimal.valueOf(2);

    @Override
    public EstrategiaCotizacion getEstrategia() {
        return EstrategiaCotizacion.RENDIMIENTO_TEMPORADA;
    }

    @Override
    public BigDecimal calcularPuntaje(
            Map<String, Map<String, BigDecimal>> metricasPorSemana,
            Map<String, BigDecimal> metricasSemanaActual,
            List<PesoMetrica> pesosActivos) {
        // Promedio ponderado: sum(peso * normalizado) / sum(peso).
        BigDecimal sumaPonderada = BigDecimal.ZERO;
        BigDecimal sumaPesos = BigDecimal.ZERO;

        for (PesoMetrica pesoMetrica : pesosActivos) {
            // Una métrica con peso pero sin dato esta semana no participa (ni suma su peso).
            BigDecimal valorActual = metricasSemanaActual.get(pesoMetrica.getClave());
            if (valorActual == null) {
                continue;
            }
            // Valores semanales de esta métrica dentro de la ventana (incluye la semana actual).
            List<BigDecimal> historicoDeLaClave =
                    metricasPorSemana.values().stream()
                            .map(
                                    metricasDeEsaSemana ->
                                            metricasDeEsaSemana.get(pesoMetrica.getClave()))
                            .filter(Objects::nonNull)
                            .toList();
            BigDecimal normalizado = normalizarMinMax(valorActual, historicoDeLaClave);
            BigDecimal normalizadoConSentido =
                    pesoMetrica.getSentido() == SentidoMetrica.NEGATIVO
                            ? normalizado.negate()
                            : normalizado;
            sumaPonderada =
                    sumaPonderada.add(pesoMetrica.getPeso().multiply(normalizadoConSentido));
            sumaPesos = sumaPesos.add(pesoMetrica.getPeso());
        }

        return sumaPesos.signum() == 0
                ? BigDecimal.ZERO
                : sumaPonderada.divide(sumaPesos, MathContext.DECIMAL64);
    }

    private static BigDecimal normalizarMinMax(BigDecimal valorActual, List<BigDecimal> historico) {
        if (historico.size() < 2) {
            return BigDecimal.ZERO;
        }
        BigDecimal min = historico.stream().min(Comparator.naturalOrder()).orElseThrow();
        BigDecimal max = historico.stream().max(Comparator.naturalOrder()).orElseThrow();
        BigDecimal rango = max.subtract(min);
        if (rango.signum() == 0) {
            return BigDecimal.ZERO;
        }
        // posicion en [0, 1] dentro del rango histórico; luego se lleva a [-1, 1] (2x - 1).
        BigDecimal posicion = valorActual.subtract(min).divide(rango, MathContext.DECIMAL64);
        return posicion.multiply(DOS).subtract(BigDecimal.ONE);
    }
}
