package com.tdetroy.valuacion.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Cubre la invariante propia de {@link RendimientoAcumuladoWhoScored} (plan.md §8.1): campos
 * obligatorios, {@code metricas} nunca vacío/blanco, y que {@link
 * RendimientoAcumuladoWhoScored#actualizarSnapshot} reemplaza por completo el valor anterior.
 */
class RendimientoAcumuladoWhoScoredTest {

    private static final UUID JUGADOR_ID = UUID.randomUUID();
    private static final String METRICAS = "{\"goals\":5,\"assists\":2}";
    private static final Instant FECHA = Instant.parse("2026-02-16T00:00:00Z");

    @Test
    void primeraVez_construyeCorrectamenteYGettersDevuelvenLoEsperado() {
        RendimientoAcumuladoWhoScored snapshot =
                RendimientoAcumuladoWhoScored.primeraVez(JUGADOR_ID, METRICAS, FECHA);

        assertThat(snapshot.getJugadorId()).isEqualTo(JUGADOR_ID);
        assertThat(snapshot.getMetricas()).isEqualTo(METRICAS);
        assertThat(snapshot.getFechaActualizacion()).isEqualTo(FECHA);
    }

    @Test
    void jugadorIdNulo_lanzaExcepcion() {
        assertThatNullPointerException()
                .isThrownBy(() -> RendimientoAcumuladoWhoScored.primeraVez(null, METRICAS, FECHA));
    }

    @Test
    void metricasNulas_lanzaExcepcion() {
        assertThatNullPointerException()
                .isThrownBy(
                        () -> RendimientoAcumuladoWhoScored.primeraVez(JUGADOR_ID, null, FECHA));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    void metricasVaciasOBlancas_lanzaExcepcion(String metricasInvalidas) {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                RendimientoAcumuladoWhoScored.primeraVez(
                                        JUGADOR_ID, metricasInvalidas, FECHA));
    }

    @Test
    void actualizarSnapshot_reemplazaMetricasYFecha() {
        RendimientoAcumuladoWhoScored snapshot =
                RendimientoAcumuladoWhoScored.primeraVez(JUGADOR_ID, METRICAS, FECHA);
        Instant fechaNueva = FECHA.plusSeconds(3600);

        snapshot.actualizarSnapshot("{\"goals\":8,\"assists\":3}", fechaNueva);

        assertThat(snapshot.getMetricas()).isEqualTo("{\"goals\":8,\"assists\":3}");
        assertThat(snapshot.getFechaActualizacion()).isEqualTo(fechaNueva);
    }

    @Test
    void actualizarSnapshotConMetricasInvalidas_lanzaExcepcionYNoModificaElSnapshot() {
        RendimientoAcumuladoWhoScored snapshot =
                RendimientoAcumuladoWhoScored.primeraVez(JUGADOR_ID, METRICAS, FECHA);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> snapshot.actualizarSnapshot(" ", FECHA.plusSeconds(1)));

        assertThat(snapshot.getMetricas()).isEqualTo(METRICAS);
        assertThat(snapshot.getFechaActualizacion()).isEqualTo(FECHA);
    }

    @Test
    void reconstruir_devuelveUnaInstanciaConLosMismosDatos() {
        RendimientoAcumuladoWhoScored reconstruido =
                RendimientoAcumuladoWhoScored.reconstruir(JUGADOR_ID, METRICAS, FECHA);

        assertThat(reconstruido.getJugadorId()).isEqualTo(JUGADOR_ID);
        assertThat(reconstruido.getMetricas()).isEqualTo(METRICAS);
        assertThat(reconstruido.getFechaActualizacion()).isEqualTo(FECHA);
    }
}
