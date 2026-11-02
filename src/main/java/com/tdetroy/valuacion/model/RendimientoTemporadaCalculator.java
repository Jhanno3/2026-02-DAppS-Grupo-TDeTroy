package com.tdetroy.valuacion.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Estrategia {@link EstrategiaCotizacion#RENDIMIENTO_TEMPORADA}: calcula el puntaje de rendimiento
 * en base a la consistencia y evolución temporal del propio jugador (plan.md §6.2).
 *
 * <p>Por cada {@code PesoMetrica} activo presente en la semana en curso, el valor se normaliza
 * min-max sobre su propio histórico reciente a un rango centrado en 0, {@code [-1, 1]} (mínimo
 * histórico → -1, máximo histórico → +1). Si la métrica no tiene al menos dos semanas de histórico
 * o el rango es cero, aporta 0 (neutro). Si {@link PesoMetrica#getSentido()} es {@link
 * SentidoMetrica#NEGATIVO} (tarjetas: más nunca debería mejorar la cotización), ese normalizado se
 * invierte antes de ponderar. El puntaje final es el promedio ponderado de esas normalizaciones.
 */
public class RendimientoTemporadaCalculator implements EstrategiaCotizacionCalculator {

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
