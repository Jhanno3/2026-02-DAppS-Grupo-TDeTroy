package com.tdetroy.valuacion.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Cubre la invariante propia de {@link PesoMetrica} (plan.md §2.9, §6.2): campos obligatorios,
 * {@code peso} siempre positivo (tanto al configurar como al actualizar), y que {@link
 * #activar()}/{@link #desactivar()} nunca tocan el peso configurado.
 */
class PesoMetricaTest {

    private static final String CLAVE = "rating";
    private static final BigDecimal PESO = new BigDecimal("1.5000");

    @Test
    void pesoMetricaValida_construyeCorrectamenteYGettersDevuelvenLoEsperado() {
        PesoMetrica pesoMetrica =
                PesoMetrica.configurar(CLAVE, PESO, true, SentidoMetrica.POSITIVO);

        assertThat(pesoMetrica.getClave()).isEqualTo(CLAVE);
        assertThat(pesoMetrica.getPeso()).isEqualByComparingTo(PESO);
        assertThat(pesoMetrica.isActivo()).isTrue();
        assertThat(pesoMetrica.getSentido()).isEqualTo(SentidoMetrica.POSITIVO);
    }

    @Test
    void claveNula_lanzaExcepcion() {
        assertThatNullPointerException()
                .isThrownBy(
                        () -> PesoMetrica.configurar(null, PESO, true, SentidoMetrica.POSITIVO));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    void claveVaciaOBlanca_lanzaExcepcion(String claveInvalida) {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                PesoMetrica.configurar(
                                        claveInvalida, PESO, true, SentidoMetrica.POSITIVO));
    }

    @Test
    void pesoNulo_lanzaExcepcion() {
        assertThatNullPointerException()
                .isThrownBy(
                        () -> PesoMetrica.configurar(CLAVE, null, true, SentidoMetrica.POSITIVO));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-0.01", "-1.5"})
    void pesoCeroONegativo_lanzaExcepcion(String pesoInvalido) {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                PesoMetrica.configurar(
                                        CLAVE,
                                        new BigDecimal(pesoInvalido),
                                        true,
                                        SentidoMetrica.POSITIVO));
    }

    @Test
    void sentidoNulo_lanzaExcepcion() {
        assertThatNullPointerException()
                .isThrownBy(() -> PesoMetrica.configurar(CLAVE, PESO, true, null));
    }

    @Test
    void actualizarPesoConValorValido_actualizaElPeso() {
        PesoMetrica pesoMetrica =
                PesoMetrica.configurar(CLAVE, PESO, true, SentidoMetrica.POSITIVO);

        pesoMetrica.actualizarPeso(new BigDecimal("2.0000"));

        assertThat(pesoMetrica.getPeso()).isEqualByComparingTo(new BigDecimal("2.0000"));
    }

    @Test
    void actualizarPesoConValorCeroONegativo_lanzaExcepcionYNoModificaElPesoActual() {
        PesoMetrica pesoMetrica =
                PesoMetrica.configurar(CLAVE, PESO, true, SentidoMetrica.POSITIVO);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> pesoMetrica.actualizarPeso(BigDecimal.ZERO));

        assertThat(pesoMetrica.getPeso()).isEqualByComparingTo(PESO);
    }

    @Test
    void desactivarYActivar_cambianActivoSinTocarElPeso() {
        PesoMetrica pesoMetrica =
                PesoMetrica.configurar(CLAVE, PESO, true, SentidoMetrica.POSITIVO);

        pesoMetrica.desactivar();
        assertThat(pesoMetrica.isActivo()).isFalse();
        assertThat(pesoMetrica.getPeso()).isEqualByComparingTo(PESO);

        pesoMetrica.activar();
        assertThat(pesoMetrica.isActivo()).isTrue();
        assertThat(pesoMetrica.getPeso()).isEqualByComparingTo(PESO);
    }

    @Test
    void reconstruir_devuelveUnaInstanciaConLosMismosDatos() {
        PesoMetrica reconstruido =
                PesoMetrica.reconstruir(CLAVE, PESO, false, SentidoMetrica.NEGATIVO);

        assertThat(reconstruido.getClave()).isEqualTo(CLAVE);
        assertThat(reconstruido.getPeso()).isEqualByComparingTo(PESO);
        assertThat(reconstruido.isActivo()).isFalse();
        assertThat(reconstruido.getSentido()).isEqualTo(SentidoMetrica.NEGATIVO);
    }
}
