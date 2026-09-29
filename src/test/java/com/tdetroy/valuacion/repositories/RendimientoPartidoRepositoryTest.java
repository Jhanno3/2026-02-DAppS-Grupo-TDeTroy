package com.tdetroy.valuacion.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import com.tdetroy.valuacion.model.FuenteResultado;
import com.tdetroy.valuacion.model.RendimientoPartido;
import com.tdetroy.valuacion.repositories.support.PostgresIntegrationTest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Test de integración de {@link RendimientoPartidoRepository} contra Postgres real (tasks.md T0.7,
 * constitution.md §1/§4). Sin métodos de consulta propios todavía (T3.5+ los agrega cuando {@code
 * RendimientoService}/{@code CotizacionService} los necesiten), así que este test cubre lo que hoy
 * expone: persistir un {@link RendimientoPartido} válido vía {@link RendimientoPartido#ingestar} y
 * leerlo de vuelta con los mismos valores (incluida la columna {@code jsonb}), que es append-only
 * también a nivel de persistencia, y la restricción {@code UNIQUE(partido_externo_id, jugador_id)}
 * que sostiene la deduplicación frente a una reingesta del mismo ciclo semanal (plan.md §2.3).
 */
class RendimientoPartidoRepositoryTest extends PostgresIntegrationTest {

    @Autowired private RendimientoPartidoRepository rendimientoPartidoRepository;

    @Autowired private TestEntityManager entityManager;

    @Test
    void guardarRendimientoValido_persisteYSeLeeDeVueltaConLosValoresEsperados()
            throws JSONException {
        UUID jugadorId = UUID.randomUUID();
        Instant fechaIngesta = Instant.parse("2026-02-16T02:00:00Z");
        RendimientoPartido rendimiento =
                RendimientoPartido.ingestar(
                        jugadorId,
                        "12345",
                        LocalDate.of(2026, 2, 15),
                        "2026-W07",
                        "{\"goles\":1,\"asistencias\":0}",
                        FuenteResultado.FOOTBALL_DATA,
                        fechaIngesta);

        RendimientoPartido guardado = rendimientoPartidoRepository.saveAndFlush(rendimiento);
        entityManager.clear();

        RendimientoPartido leido =
                rendimientoPartidoRepository.findById(guardado.getId()).orElseThrow();

        assertThat(leido.getId()).isEqualTo(rendimiento.getId());
        assertThat(leido.getJugadorId()).isEqualTo(jugadorId);
        assertThat(leido.getPartidoExternoId()).isEqualTo("12345");
        assertThat(leido.getFechaPartido()).isEqualTo(LocalDate.of(2026, 2, 15));
        assertThat(leido.getSemanaCalculo()).isEqualTo("2026-W07");
        assertEquals(
                "{\"goles\":1,\"asistencias\":0}", leido.getMetricas(), JSONCompareMode.STRICT);
        assertThat(leido.getFuenteResultado()).isEqualTo(FuenteResultado.FOOTBALL_DATA);
        assertThat(leido.getFechaIngesta()).isEqualTo(fechaIngesta);
    }

    @Test
    void dosRendimientosGuardadosPorSeparado_persistenComoFilasIndependientesSinSobrescribirse() {
        RendimientoPartido primero =
                RendimientoPartido.ingestar(
                        UUID.randomUUID(),
                        "111",
                        LocalDate.of(2026, 2, 15),
                        "2026-W07",
                        "{\"goles\":1}",
                        FuenteResultado.FOOTBALL_DATA);
        RendimientoPartido segundo =
                RendimientoPartido.ingestar(
                        UUID.randomUUID(),
                        "222",
                        LocalDate.of(2026, 2, 16),
                        "2026-W07",
                        "{\"goles\":0}",
                        FuenteResultado.FOOTBALL_DATA);

        rendimientoPartidoRepository.saveAndFlush(primero);
        rendimientoPartidoRepository.saveAndFlush(segundo);
        entityManager.clear();

        assertThat(rendimientoPartidoRepository.count()).isEqualTo(2);
        assertThat(primero.getId()).isNotEqualTo(segundo.getId());
    }

    @Test
    void mismoPartidoExternoYJugador_violaRestriccionDeUnicidad() {
        UUID jugadorId = UUID.randomUUID();
        RendimientoPartido primero =
                RendimientoPartido.ingestar(
                        jugadorId,
                        "999",
                        LocalDate.of(2026, 2, 15),
                        "2026-W07",
                        "{\"goles\":1}",
                        FuenteResultado.FOOTBALL_DATA);
        RendimientoPartido duplicado =
                RendimientoPartido.ingestar(
                        jugadorId,
                        "999",
                        LocalDate.of(2026, 2, 15),
                        "2026-W07",
                        "{\"goles\":1}",
                        FuenteResultado.FOOTBALL_DATA);

        rendimientoPartidoRepository.saveAndFlush(primero);

        assertThatThrownBy(() -> rendimientoPartidoRepository.saveAndFlush(duplicado))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
