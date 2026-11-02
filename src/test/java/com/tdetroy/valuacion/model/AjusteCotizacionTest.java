package com.tdetroy.valuacion.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AjusteCotizacionTest {

    private static final BigDecimal SENSIBILIDAD = new BigDecimal("0.20");
    private static final BigDecimal DECAY = new BigDecimal("0.01");

    @Test
    void porRendimiento_escalaElPuntajePorLaSensibilidad() {
        AjusteCotizacion ajuste =
                AjusteCotizacion.porRendimiento(new BigDecimal("0.5"), SENSIBILIDAD);

        assertThat(ajuste.getFactor()).isEqualByComparingTo("0.10");
    }

    @Test
    void porRendimiento_acotaLaSubaAlVeintePorCiento() {
        AjusteCotizacion ajuste =
                AjusteCotizacion.porRendimiento(new BigDecimal("1"), new BigDecimal("5"));

        assertThat(ajuste.getFactor()).isEqualByComparingTo(AjusteCotizacion.VARIACION_MAXIMA);
    }

    @Test
    void porRendimiento_acotaLaBajaAlVeintePorCiento() {
        AjusteCotizacion ajuste =
                AjusteCotizacion.porRendimiento(new BigDecimal("-1"), new BigDecimal("5"));

        assertThat(ajuste.getFactor())
                .isEqualByComparingTo(AjusteCotizacion.VARIACION_MAXIMA.negate());
    }

    @Test
    void porInactividad_esUnaBajaFija() {
        assertThat(AjusteCotizacion.porInactividad(DECAY).getFactor())
                .isEqualByComparingTo("-0.01");
    }

    @Test
    void porInactividad_conDecayNegativo_lanzaExcepcion() {
        assertThatThrownBy(() -> AjusteCotizacion.porInactividad(new BigDecimal("-0.01")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aplicarA_multiplicaPorUnoMasElFactorYRedondeaADosDecimales() {
        AjusteCotizacion ajuste =
                AjusteCotizacion.porRendimiento(new BigDecimal("0.5"), SENSIBILIDAD);

        // 100.00 × (1 + 0.10) = 110.00
        assertThat(ajuste.aplicarA(new BigDecimal("100.00"))).isEqualTo(new BigDecimal("110.00"));
        // 33.33 × 1.10 = 36.663 -> 36.66
        assertThat(ajuste.aplicarA(new BigDecimal("33.33"))).isEqualTo(new BigDecimal("36.66"));
    }

    @Test
    void calcular_sinMetricasEnLaSemana_aplicaDecayPorInactividad() {
        AjusteCotizacion ajuste =
                AjusteCotizacion.calcular(
                        EstrategiaCotizacion.RENDIMIENTO_ACTUAL,
                        Map.of(),
                        Map.of(),
                        List.of(pesoGoles()),
                        SENSIBILIDAD,
                        DECAY);

        assertThat(ajuste.getFactor()).isEqualByComparingTo("-0.01");
    }

    @Test
    void calcular_conMetricas_usaElPuntajeDeLaEstrategia() {
        // RENDIMIENTO_ACTUAL: 1 gol = 0.5 -> 0.5 × 0.20 = +10%
        Map<String, BigDecimal> semanaActual = Map.of("goals", BigDecimal.ONE);

        AjusteCotizacion ajuste =
                AjusteCotizacion.calcular(
                        EstrategiaCotizacion.RENDIMIENTO_ACTUAL,
                        Map.of("2026-W44", semanaActual),
                        semanaActual,
                        List.of(pesoGoles()),
                        SENSIBILIDAD,
                        DECAY);

        assertThat(ajuste.getFactor()).isEqualByComparingTo("0.10");
    }

    private static PesoMetrica pesoGoles() {
        return PesoMetrica.configurar("goals", BigDecimal.ONE, true, SentidoMetrica.POSITIVO);
    }
}
