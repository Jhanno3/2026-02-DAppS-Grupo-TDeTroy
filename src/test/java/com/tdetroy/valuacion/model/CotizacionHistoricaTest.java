package com.tdetroy.valuacion.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Cubre la invariante propia de {@link CotizacionHistorica} (plan.md §2.4): campos siempre
 * obligatorios, formato ISO week de {@code semana}, {@code valor} siempre positivo y escalado vía
 * {@link com.tdetroy.valuacion.common.Monetario}, y que dos cálculos sucesivos nunca colisionan en
 * id (append-only, nunca sobrescribe el registro anterior).
 */
class CotizacionHistoricaTest {

    private static final UUID JUGADOR_ID = UUID.randomUUID();
    private static final String SEMANA = "2026-W07";
    private static final BigDecimal VALOR = new BigDecimal("105.50");
    private static final Instant FECHA_CALCULO_FIJA = Instant.parse("2026-02-16T03:00:00Z");

    @Test
    void cotizacionValida_construyeCorrectamenteYGettersDevuelvenLoEsperado() {
        CotizacionHistorica cotizacion =
                CotizacionHistorica.calcular(
                        JUGADOR_ID, SEMANA, VALOR, OrigenCotizacion.AUTOMATICO, FECHA_CALCULO_FIJA);

        assertThat(cotizacion.getId()).isNotNull();
        assertThat(cotizacion.getJugadorId()).isEqualTo(JUGADOR_ID);
        assertThat(cotizacion.getSemana()).isEqualTo(SEMANA);
        assertThat(cotizacion.getValor()).isEqualByComparingTo(VALOR);
        assertThat(cotizacion.getOrigen()).isEqualTo(OrigenCotizacion.AUTOMATICO);
        assertThat(cotizacion.getFechaCalculo()).isEqualTo(FECHA_CALCULO_FIJA);
    }

    @Test
    void valorSinEscalarAEscalaMonetaria_seEscalaAlConstruir() {
        CotizacionHistorica cotizacion =
                CotizacionHistorica.calcular(
                        JUGADOR_ID,
                        SEMANA,
                        new BigDecimal("100.005"),
                        OrigenCotizacion.MANUAL,
                        FECHA_CALCULO_FIJA);

        assertThat(cotizacion.getValor()).isEqualByComparingTo(new BigDecimal("100.01"));
        assertThat(cotizacion.getValor().scale()).isEqualTo(2);
    }

    @Test
    void jugadorIdNulo_lanzaExcepcion() {
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                CotizacionHistorica.calcular(
                                        null,
                                        SEMANA,
                                        VALOR,
                                        OrigenCotizacion.AUTOMATICO,
                                        FECHA_CALCULO_FIJA));
    }

    @Test
    void semanaNula_lanzaExcepcion() {
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                CotizacionHistorica.calcular(
                                        JUGADOR_ID,
                                        null,
                                        VALOR,
                                        OrigenCotizacion.AUTOMATICO,
                                        FECHA_CALCULO_FIJA));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-07", "26-W07", "2026-W7", "semana7", ""})
    void semanaConFormatoInvalido_lanzaExcepcion(String semanaInvalida) {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                CotizacionHistorica.calcular(
                                        JUGADOR_ID,
                                        semanaInvalida,
                                        VALOR,
                                        OrigenCotizacion.AUTOMATICO,
                                        FECHA_CALCULO_FIJA));
    }

    @Test
    void valorNulo_lanzaExcepcion() {
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                CotizacionHistorica.calcular(
                                        JUGADOR_ID,
                                        SEMANA,
                                        null,
                                        OrigenCotizacion.AUTOMATICO,
                                        FECHA_CALCULO_FIJA));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-0.01", "-100.00"})
    void valorCeroONegativo_lanzaExcepcion(String valorInvalido) {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                CotizacionHistorica.calcular(
                                        JUGADOR_ID,
                                        SEMANA,
                                        new BigDecimal(valorInvalido),
                                        OrigenCotizacion.AUTOMATICO,
                                        FECHA_CALCULO_FIJA));
    }

    @Test
    void origenNulo_lanzaExcepcion() {
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                CotizacionHistorica.calcular(
                                        JUGADOR_ID, SEMANA, VALOR, null, FECHA_CALCULO_FIJA));
    }

    @Test
    void fechaCalculoNula_lanzaExcepcion() {
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                CotizacionHistorica.calcular(
                                        JUGADOR_ID,
                                        SEMANA,
                                        VALOR,
                                        OrigenCotizacion.AUTOMATICO,
                                        null));
    }

    @Test
    void calcularSinFechaExplicita_usaInstanteActual() {
        Instant antes = Instant.now();
        CotizacionHistorica cotizacion =
                CotizacionHistorica.calcular(
                        JUGADOR_ID, SEMANA, VALOR, OrigenCotizacion.AUTOMATICO);
        Instant despues = Instant.now();

        assertThat(cotizacion.getFechaCalculo()).isBetween(antes, despues);
    }

    @Test
    void dosCotizacionesCalculadasPorSeparadoTienenIdsDistintos() {
        CotizacionHistorica primera =
                CotizacionHistorica.calcular(
                        JUGADOR_ID, SEMANA, VALOR, OrigenCotizacion.AUTOMATICO, FECHA_CALCULO_FIJA);
        CotizacionHistorica segunda =
                CotizacionHistorica.calcular(
                        JUGADOR_ID, SEMANA, VALOR, OrigenCotizacion.AUTOMATICO, FECHA_CALCULO_FIJA);

        assertThat(primera.getId()).isNotEqualTo(segunda.getId());
    }
}
