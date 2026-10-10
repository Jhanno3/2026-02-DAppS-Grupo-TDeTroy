package com.tdetroy.valuacion.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import com.tdetroy.valuacion.entity.CotizacionHistoricaEntity;
import com.tdetroy.valuacion.model.CotizacionHistorica;
import com.tdetroy.valuacion.model.OrigenCotizacion;
import com.tdetroy.valuacion.repositories.support.PostgresIntegrationTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * Test de integración de {@link CotizacionHistoricaRepository} contra Postgres real (tasks.md T4.1,
 * constitution.md §1/§4): persistir una {@link CotizacionHistorica} válida vía {@link
 * CotizacionHistorica#calcular} y leerla de vuelta con los mismos valores, y que dos cálculos
 * sucesivos del mismo jugador (ej. semanas distintas, o un recálculo manual sobre la misma semana)
 * persisten como filas independientes sin pisarse — append-only también a nivel de persistencia
 * (plan.md §2.4: nunca {@code UPDATE}, "vigente" es siempre el último registro por {@code
 * fechaCalculo}).
 */
class CotizacionHistoricaRepositoryIT extends PostgresIntegrationTest {

    @Autowired private CotizacionHistoricaRepository cotizacionHistoricaRepository;

    @Autowired private TestEntityManager entityManager;

    @Test
    void guardarCotizacionValida_persisteYSeLeeDeVueltaConLosValoresEsperados() {
        UUID jugadorId = UUID.randomUUID();
        Instant fechaCalculo = Instant.parse("2026-02-16T03:00:00Z");
        CotizacionHistorica cotizacion =
                CotizacionHistorica.calcular(
                        jugadorId,
                        "2026-W07",
                        new BigDecimal("105.50"),
                        OrigenCotizacion.AUTOMATICO,
                        fechaCalculo);

        CotizacionHistoricaEntity guardada =
                cotizacionHistoricaRepository.saveAndFlush(
                        CotizacionHistoricaEntity.desde(cotizacion));
        entityManager.clear();

        CotizacionHistorica leida =
                cotizacionHistoricaRepository.findById(guardada.getId()).orElseThrow().aModelo();

        assertThat(leida.getId()).isEqualTo(cotizacion.getId());
        assertThat(leida.getJugadorId()).isEqualTo(jugadorId);
        assertThat(leida.getSemana()).isEqualTo("2026-W07");
        assertThat(leida.getValor()).isEqualByComparingTo(new BigDecimal("105.50"));
        assertThat(leida.getOrigen()).isEqualTo(OrigenCotizacion.AUTOMATICO);
        assertThat(leida.getFechaCalculo()).isEqualTo(fechaCalculo);
    }

    @Test
    void dosCalculosDelMismoJugador_persistenComoFilasIndependientesSinSobrescribirse() {
        UUID jugadorId = UUID.randomUUID();
        CotizacionHistorica primera =
                CotizacionHistorica.calcular(
                        jugadorId,
                        "2026-W06",
                        new BigDecimal("100.00"),
                        OrigenCotizacion.AUTOMATICO,
                        Instant.parse("2026-02-09T03:00:00Z"));
        CotizacionHistorica segunda =
                CotizacionHistorica.calcular(
                        jugadorId,
                        "2026-W07",
                        new BigDecimal("105.50"),
                        OrigenCotizacion.AUTOMATICO,
                        Instant.parse("2026-02-16T03:00:00Z"));

        cotizacionHistoricaRepository.saveAndFlush(CotizacionHistoricaEntity.desde(primera));
        cotizacionHistoricaRepository.saveAndFlush(CotizacionHistoricaEntity.desde(segunda));
        entityManager.clear();

        assertThat(cotizacionHistoricaRepository.count()).isEqualTo(2);
        assertThat(primera.getId()).isNotEqualTo(segunda.getId());
        assertThat(cotizacionHistoricaRepository.findById(primera.getId()).orElseThrow().getValor())
                .isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(cotizacionHistoricaRepository.findById(segunda.getId()).orElseThrow().getValor())
                .isEqualByComparingTo(new BigDecimal("105.50"));
    }
}
