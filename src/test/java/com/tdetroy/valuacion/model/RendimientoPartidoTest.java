package com.tdetroy.valuacion.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Cubre la invariante propia de {@link RendimientoPartido} (plan.md §2.3): campos siempre
 * obligatorios y el formato ISO week de {@code semanaCalculo}.
 */
class RendimientoPartidoTest {

    private static final UUID JUGADOR_ID = UUID.randomUUID();
    private static final String PARTIDO_EXTERNO_ID = "12345";
    private static final LocalDate FECHA_PARTIDO = LocalDate.of(2026, 2, 15);
    private static final String SEMANA_CALCULO = "2026-W07";
    private static final String METRICAS = "{\"goles\":1,\"asistencias\":0}";
    private static final Instant FECHA_INGESTA_FIJA = Instant.parse("2026-02-16T02:00:00Z");

    @Test
    void rendimientoValido_construyeCorrectamenteYGettersDevuelvenLoEsperado() {
        RendimientoPartido rendimiento =
                RendimientoPartido.ingestar(
                        JUGADOR_ID,
                        PARTIDO_EXTERNO_ID,
                        FECHA_PARTIDO,
                        SEMANA_CALCULO,
                        METRICAS,
                        FuenteResultado.FOOTBALL_DATA,
                        FECHA_INGESTA_FIJA);

        assertThat(rendimiento.getId()).isNotNull();
        assertThat(rendimiento.getJugadorId()).isEqualTo(JUGADOR_ID);
        assertThat(rendimiento.getPartidoExternoId()).isEqualTo(PARTIDO_EXTERNO_ID);
        assertThat(rendimiento.getFechaPartido()).isEqualTo(FECHA_PARTIDO);
        assertThat(rendimiento.getSemanaCalculo()).isEqualTo(SEMANA_CALCULO);
        assertThat(rendimiento.getMetricas()).isEqualTo(METRICAS);
        assertThat(rendimiento.getFuenteResultado()).isEqualTo(FuenteResultado.FOOTBALL_DATA);
        assertThat(rendimiento.getFechaIngesta()).isEqualTo(FECHA_INGESTA_FIJA);
    }

    @Test
    void jugadorIdNulo_lanzaExcepcion() {
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                RendimientoPartido.ingestar(
                                        null,
                                        PARTIDO_EXTERNO_ID,
                                        FECHA_PARTIDO,
                                        SEMANA_CALCULO,
                                        METRICAS,
                                        FuenteResultado.FOOTBALL_DATA,
                                        FECHA_INGESTA_FIJA));
    }

    @Test
    void partidoExternoIdNulo_lanzaExcepcion() {
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                RendimientoPartido.ingestar(
                                        JUGADOR_ID,
                                        null,
                                        FECHA_PARTIDO,
                                        SEMANA_CALCULO,
                                        METRICAS,
                                        FuenteResultado.FOOTBALL_DATA,
                                        FECHA_INGESTA_FIJA));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    void partidoExternoIdVacioOBlanco_lanzaExcepcion(String partidoExternoIdInvalido) {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                RendimientoPartido.ingestar(
                                        JUGADOR_ID,
                                        partidoExternoIdInvalido,
                                        FECHA_PARTIDO,
                                        SEMANA_CALCULO,
                                        METRICAS,
                                        FuenteResultado.FOOTBALL_DATA,
                                        FECHA_INGESTA_FIJA));
    }

    @Test
    void fechaPartidoNula_lanzaExcepcion() {
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                RendimientoPartido.ingestar(
                                        JUGADOR_ID,
                                        PARTIDO_EXTERNO_ID,
                                        null,
                                        SEMANA_CALCULO,
                                        METRICAS,
                                        FuenteResultado.FOOTBALL_DATA,
                                        FECHA_INGESTA_FIJA));
    }

    @Test
    void semanaCalculoNula_lanzaExcepcion() {
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                RendimientoPartido.ingestar(
                                        JUGADOR_ID,
                                        PARTIDO_EXTERNO_ID,
                                        FECHA_PARTIDO,
                                        null,
                                        METRICAS,
                                        FuenteResultado.FOOTBALL_DATA,
                                        FECHA_INGESTA_FIJA));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-07", "26-W07", "2026-W7", "semana7", ""})
    void semanaCalculoConFormatoInvalido_lanzaExcepcion(String semanaInvalida) {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                RendimientoPartido.ingestar(
                                        JUGADOR_ID,
                                        PARTIDO_EXTERNO_ID,
                                        FECHA_PARTIDO,
                                        semanaInvalida,
                                        METRICAS,
                                        FuenteResultado.FOOTBALL_DATA,
                                        FECHA_INGESTA_FIJA));
    }

    @Test
    void metricasNulas_lanzaExcepcion() {
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                RendimientoPartido.ingestar(
                                        JUGADOR_ID,
                                        PARTIDO_EXTERNO_ID,
                                        FECHA_PARTIDO,
                                        SEMANA_CALCULO,
                                        null,
                                        FuenteResultado.FOOTBALL_DATA,
                                        FECHA_INGESTA_FIJA));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    void metricasVaciasOBlanco_lanzaExcepcion(String metricasInvalidas) {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                RendimientoPartido.ingestar(
                                        JUGADOR_ID,
                                        PARTIDO_EXTERNO_ID,
                                        FECHA_PARTIDO,
                                        SEMANA_CALCULO,
                                        metricasInvalidas,
                                        FuenteResultado.FOOTBALL_DATA,
                                        FECHA_INGESTA_FIJA));
    }

    @Test
    void fuenteResultadoNula_lanzaExcepcion() {
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                RendimientoPartido.ingestar(
                                        JUGADOR_ID,
                                        PARTIDO_EXTERNO_ID,
                                        FECHA_PARTIDO,
                                        SEMANA_CALCULO,
                                        METRICAS,
                                        null,
                                        FECHA_INGESTA_FIJA));
    }

    @Test
    void fechaIngestaNula_lanzaExcepcion() {
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                RendimientoPartido.ingestar(
                                        JUGADOR_ID,
                                        PARTIDO_EXTERNO_ID,
                                        FECHA_PARTIDO,
                                        SEMANA_CALCULO,
                                        METRICAS,
                                        FuenteResultado.FOOTBALL_DATA,
                                        null));
    }

    @Test
    void ingestarSinFechaExplicita_usaInstanteActual() {
        Instant antes = Instant.now();
        RendimientoPartido rendimiento =
                RendimientoPartido.ingestar(
                        JUGADOR_ID,
                        PARTIDO_EXTERNO_ID,
                        FECHA_PARTIDO,
                        SEMANA_CALCULO,
                        METRICAS,
                        FuenteResultado.FOOTBALL_DATA);
        Instant despues = Instant.now();

        assertThat(rendimiento.getFechaIngesta()).isBetween(antes, despues);
    }

    @Test
    void dosRendimientosIngestadosPorSeparadoTienenIdsDistintos() {
        RendimientoPartido uno =
                RendimientoPartido.ingestar(
                        JUGADOR_ID,
                        PARTIDO_EXTERNO_ID,
                        FECHA_PARTIDO,
                        SEMANA_CALCULO,
                        METRICAS,
                        FuenteResultado.FOOTBALL_DATA,
                        FECHA_INGESTA_FIJA);
        RendimientoPartido otro =
                RendimientoPartido.ingestar(
                        JUGADOR_ID,
                        PARTIDO_EXTERNO_ID,
                        FECHA_PARTIDO,
                        SEMANA_CALCULO,
                        METRICAS,
                        FuenteResultado.FOOTBALL_DATA,
                        FECHA_INGESTA_FIJA);

        assertThat(uno.getId()).isNotEqualTo(otro.getId());
    }
}
