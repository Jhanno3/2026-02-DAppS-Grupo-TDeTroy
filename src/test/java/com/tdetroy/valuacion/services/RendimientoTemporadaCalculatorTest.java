package com.tdetroy.valuacion.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.tdetroy.valuacion.entity.PesoMetricaEntity;
import com.tdetroy.valuacion.model.EstrategiaCotizacion;
import com.tdetroy.valuacion.model.SentidoMetrica;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RendimientoTemporadaCalculatorTest {

    private RendimientoTemporadaCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new RendimientoTemporadaCalculator();
    }

    @Test
    void getEstrategia_retornaRendimientoTemporada() {
        assertThat(calculator.getEstrategia())
                .isEqualTo(EstrategiaCotizacion.RENDIMIENTO_TEMPORADA);
    }

    @Test
    void sinPesosActivos_retornaCero() {
        Map<String, Map<String, BigDecimal>> historico =
                Map.of("2026-W01", Map.of("goles", new BigDecimal("1")));
        Map<String, BigDecimal> semanaActual = Map.of("goles", new BigDecimal("1"));

        BigDecimal puntaje = calculator.calcularPuntaje(historico, semanaActual, List.of());

        assertThat(puntaje).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void conMenosDeDosSemanasHistoricas_metricaAportaCero() {
        Map<String, Map<String, BigDecimal>> historico =
                Map.of("2026-W01", Map.of("goles", new BigDecimal("2")));
        Map<String, BigDecimal> semanaActual = Map.of("goles", new BigDecimal("2"));
        PesoMetricaEntity pesoGoles = new PesoMetricaEntity();
        pesoGoles.setClave("goles");
        pesoGoles.setPeso(new BigDecimal("1.0"));
        pesoGoles.setActivo(true);

        BigDecimal puntaje =
                calculator.calcularPuntaje(historico, semanaActual, List.of(pesoGoles));

        assertThat(puntaje).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void valorMaximoHistorico_normalizaAUno() {
        Map<String, Map<String, BigDecimal>> historico =
                Map.of(
                        "2026-W01", Map.of("goles", new BigDecimal("0")),
                        "2026-W02", Map.of("goles", new BigDecimal("2")));
        Map<String, BigDecimal> semanaActual = Map.of("goles", new BigDecimal("2"));
        PesoMetricaEntity pesoGoles = new PesoMetricaEntity();
        pesoGoles.setClave("goles");
        pesoGoles.setPeso(new BigDecimal("1.0"));
        pesoGoles.setActivo(true);

        BigDecimal puntaje =
                calculator.calcularPuntaje(historico, semanaActual, List.of(pesoGoles));

        // En maximo: normalizado a +1
        assertThat(puntaje).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void valorMinimoHistorico_normalizaAMenosUno() {
        Map<String, Map<String, BigDecimal>> historico =
                Map.of(
                        "2026-W01", Map.of("goles", new BigDecimal("0")),
                        "2026-W02", Map.of("goles", new BigDecimal("2")));
        Map<String, BigDecimal> semanaActual = Map.of("goles", new BigDecimal("0"));
        PesoMetricaEntity pesoGoles = new PesoMetricaEntity();
        pesoGoles.setClave("goles");
        pesoGoles.setPeso(new BigDecimal("1.0"));
        pesoGoles.setActivo(true);

        BigDecimal puntaje =
                calculator.calcularPuntaje(historico, semanaActual, List.of(pesoGoles));

        // En minimo: normalizado a -1
        assertThat(puntaje).isEqualByComparingTo(BigDecimal.ONE.negate());
    }

    @Test
    void conMetricaDeSentidoNegativo_invierteElSigno() {
        Map<String, Map<String, BigDecimal>> historico =
                Map.of(
                        "2026-W01", Map.of("redCard", new BigDecimal("0")),
                        "2026-W02", Map.of("redCard", new BigDecimal("2")));
        Map<String, BigDecimal> semanaActual = Map.of("redCard", new BigDecimal("2"));
        PesoMetricaEntity pesoRedCard = new PesoMetricaEntity();
        pesoRedCard.setClave("redCard");
        pesoRedCard.setPeso(new BigDecimal("1.0"));
        pesoRedCard.setActivo(true);
        pesoRedCard.setSentido(SentidoMetrica.NEGATIVO);

        BigDecimal puntaje =
                calculator.calcularPuntaje(historico, semanaActual, List.of(pesoRedCard));

        // En maximo historico de tarjetas, sin sentido normalizaria a +1; con NEGATIVO, debe ser -1
        assertThat(puntaje).isEqualByComparingTo(BigDecimal.ONE.negate());
    }
}
