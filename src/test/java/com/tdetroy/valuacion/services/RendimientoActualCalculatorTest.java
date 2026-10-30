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

class RendimientoActualCalculatorTest {

    private RendimientoActualCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new RendimientoActualCalculator();
    }

    @Test
    void getEstrategia_retornaRendimientoActual() {
        assertThat(calculator.getEstrategia()).isEqualTo(EstrategiaCotizacion.RENDIMIENTO_ACTUAL);
    }

    @Test
    void semanaActualVacia_retornaCero() {
        BigDecimal puntaje = calculator.calcularPuntaje(Map.of(), Map.of(), List.of());
        assertThat(puntaje).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void conRatingNeutroDeSeisPuntos_retornaCero() {
        Map<String, BigDecimal> semanaActual = Map.of("rating", new BigDecimal("6.0"));
        PesoMetricaEntity pesoRating = new PesoMetricaEntity();
        pesoRating.setClave("rating");
        pesoRating.setPeso(new BigDecimal("1.0"));
        pesoRating.setActivo(true);

        BigDecimal puntaje =
                calculator.calcularPuntaje(Map.of(), semanaActual, List.of(pesoRating));

        // (6.0 - 6.0) / 4.0 = 0.0
        assertThat(puntaje).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void conRatingSobresalienteDeDiez_retornaUno() {
        Map<String, BigDecimal> semanaActual = Map.of("rating", new BigDecimal("10.0"));
        PesoMetricaEntity pesoRating = new PesoMetricaEntity();
        pesoRating.setClave("rating");
        pesoRating.setPeso(new BigDecimal("1.0"));
        pesoRating.setActivo(true);

        BigDecimal puntaje =
                calculator.calcularPuntaje(Map.of(), semanaActual, List.of(pesoRating));

        // (10.0 - 6.0) / 4.0 = 1.0
        assertThat(puntaje).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void conRatingBajoDeDos_retornaMenosUno() {
        Map<String, BigDecimal> semanaActual = Map.of("rating", new BigDecimal("2.0"));
        PesoMetricaEntity pesoRating = new PesoMetricaEntity();
        pesoRating.setClave("rating");
        pesoRating.setPeso(new BigDecimal("1.0"));
        pesoRating.setActivo(true);

        BigDecimal puntaje =
                calculator.calcularPuntaje(Map.of(), semanaActual, List.of(pesoRating));

        // (2.0 - 6.0) / 4.0 = -1.0
        assertThat(puntaje).isEqualByComparingTo(BigDecimal.ONE.negate());
    }

    @Test
    void conGolesYAsistencias_calculaPromedioPonderadoPositivo() {
        Map<String, BigDecimal> semanaActual =
                Map.of("goles", new BigDecimal("2"), "asistencias", new BigDecimal("1"));
        PesoMetricaEntity pesoGoles = new PesoMetricaEntity();
        pesoGoles.setClave("goles");
        pesoGoles.setPeso(new BigDecimal("2.0"));
        pesoGoles.setActivo(true);

        PesoMetricaEntity pesoAsistencias = new PesoMetricaEntity();
        pesoAsistencias.setClave("asistencias");
        pesoAsistencias.setPeso(new BigDecimal("1.0"));
        pesoAsistencias.setActivo(true);

        BigDecimal puntaje =
                calculator.calcularPuntaje(
                        Map.of(), semanaActual, List.of(pesoGoles, pesoAsistencias));

        // goles = 2 * 0.5 = 1.0 (peso 2) -> 2.0
        // asistencias = 1 * 0.4 = 0.4 (peso 1) -> 0.4
        // total = 2.4 / 3 = 0.8
        assertThat(puntaje).isEqualByComparingTo(new BigDecimal("0.8"));
    }

    @Test
    void conMetricaDeSentidoNegativo_invierteElSigno() {
        Map<String, BigDecimal> semanaActual = Map.of("redCard", new BigDecimal("1"));
        PesoMetricaEntity pesoRedCard = new PesoMetricaEntity();
        pesoRedCard.setClave("redCard");
        pesoRedCard.setPeso(new BigDecimal("1.0"));
        pesoRedCard.setActivo(true);
        pesoRedCard.setSentido(SentidoMetrica.NEGATIVO);

        BigDecimal puntaje =
                calculator.calcularPuntaje(Map.of(), semanaActual, List.of(pesoRedCard));

        // sin sentido, 1 tarjeta roja puntuaria +0.2 (1/5); con NEGATIVO, debe puntuar -0.2
        assertThat(puntaje).isEqualByComparingTo(new BigDecimal("-0.2"));
    }
}
