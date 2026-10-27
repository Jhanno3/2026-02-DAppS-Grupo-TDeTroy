package com.tdetroy.valuacion.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import com.tdetroy.valuacion.entity.RendimientoAcumuladoWhoScoredEntity;
import com.tdetroy.valuacion.model.RendimientoAcumuladoWhoScored;
import com.tdetroy.valuacion.repositories.support.PostgresIntegrationTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * Test de integración de {@link RendimientoAcumuladoWhoScoredRepository} contra Postgres real
 * (plan.md §8.1, constitution.md §1/§4): persistir un snapshot vía {@link
 * RendimientoAcumuladoWhoScored#primeraVez} y leerlo de vuelta, y que reemplazar el snapshot de un
 * jugador ya persistido (vía {@link RendimientoAcumuladoWhoScored#actualizarSnapshot}) actualiza
 * esa misma fila en vez de insertar una nueva — {@code jugadorId} es la clave primaria natural,
 * mismo criterio que {@link PesoMetricaRepositoryIT} con {@code clave}. Las comparaciones sobre
 * {@code metricas} usan {@link org.skyscreamer.jsonassert.JSONAssert} (mismo criterio que {@code
 * RegistroAuditoriaRepositoryIT}): Postgres no preserva el texto original de un {@code jsonb}
 * (reescribe espaciado), así que comparar como String exacto es frágil.
 */
class RendimientoAcumuladoWhoScoredRepositoryIT extends PostgresIntegrationTest {

    @Autowired private RendimientoAcumuladoWhoScoredRepository repository;

    @Autowired private TestEntityManager entityManager;

    @Test
    void guardarSnapshotValido_persisteYSeLeeDeVueltaConLosValoresEsperados() throws JSONException {
        UUID jugadorId = UUID.randomUUID();
        Instant fecha = Instant.now().truncatedTo(ChronoUnit.MICROS);
        RendimientoAcumuladoWhoScored snapshot =
                RendimientoAcumuladoWhoScored.primeraVez(
                        jugadorId, "{\"goals\":5,\"assists\":2}", fecha);

        repository.saveAndFlush(RendimientoAcumuladoWhoScoredEntity.desde(snapshot));
        entityManager.clear();

        RendimientoAcumuladoWhoScored leido =
                repository.findById(jugadorId).orElseThrow().aModelo();

        assertThat(leido.getJugadorId()).isEqualTo(jugadorId);
        assertEquals("{\"goals\":5,\"assists\":2}", leido.getMetricas(), JSONCompareMode.STRICT);
        assertThat(leido.getFechaActualizacion()).isEqualTo(fecha);
    }

    @Test
    void actualizarSnapshot_persisteSobreLaMismaFila() throws JSONException {
        UUID jugadorId = UUID.randomUUID();
        RendimientoAcumuladoWhoScored snapshot =
                RendimientoAcumuladoWhoScored.primeraVez(jugadorId, "{\"goals\":5}");
        repository.saveAndFlush(RendimientoAcumuladoWhoScoredEntity.desde(snapshot));
        entityManager.clear();

        snapshot.actualizarSnapshot("{\"goals\":8}", Instant.now().truncatedTo(ChronoUnit.MICROS));
        repository.saveAndFlush(RendimientoAcumuladoWhoScoredEntity.desde(snapshot));
        entityManager.clear();

        assertThat(repository.count()).isEqualTo(1);
        RendimientoAcumuladoWhoScored actualizado =
                repository.findById(jugadorId).orElseThrow().aModelo();
        assertEquals("{\"goals\":8}", actualizado.getMetricas(), JSONCompareMode.STRICT);
    }

    @Test
    void findById_sinSnapshotPrevio_devuelveVacio() {
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }
}
