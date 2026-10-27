package com.tdetroy.valuacion.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import com.tdetroy.valuacion.entity.CotizacionHistoricaEntity;
import com.tdetroy.valuacion.model.CotizacionHistorica;
import com.tdetroy.valuacion.model.OrigenCotizacion;
import com.tdetroy.valuacion.repositories.support.PostgresIntegrationTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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

    // ---- findValoresVigentes / findValoresIniciales (plan.md §3bis, listado de Mercado) ----

    @Test
    void findValoresVigentes_conVariosJugadoresYVariosCalculosCadaUno_devuelveSoloElMasReciente() {
        UUID jugadorA = UUID.randomUUID();
        UUID jugadorB = UUID.randomUUID();
        guardar(jugadorA, "2026-W06", "100.00", Instant.parse("2026-02-09T03:00:00Z"));
        guardar(jugadorA, "2026-W07", "105.50", Instant.parse("2026-02-16T03:00:00Z"));
        guardar(jugadorB, "2026-W07", "80.00", Instant.parse("2026-02-16T03:00:00Z"));
        entityManager.clear();

        List<ValorPorJugadorProjection> vigentes =
                cotizacionHistoricaRepository.findValoresVigentes();

        assertThat(vigentes).hasSize(2);
        assertThat(valorDe(vigentes, jugadorA)).isEqualByComparingTo("105.50");
        assertThat(valorDe(vigentes, jugadorB)).isEqualByComparingTo("80.00");
    }

    @Test
    void findValoresIniciales_conVariosCalculosDelMismoJugador_devuelveElMasAntiguo() {
        UUID jugadorA = UUID.randomUUID();
        guardar(jugadorA, "2026-W06", "100.00", Instant.parse("2026-02-09T03:00:00Z"));
        guardar(jugadorA, "2026-W07", "105.50", Instant.parse("2026-02-16T03:00:00Z"));
        entityManager.clear();

        List<ValorPorJugadorProjection> iniciales =
                cotizacionHistoricaRepository.findValoresIniciales();

        assertThat(valorDe(iniciales, jugadorA)).isEqualByComparingTo("100.00");
    }

    private void guardar(UUID jugadorId, String semana, String valor, Instant fechaCalculo) {
        cotizacionHistoricaRepository.saveAndFlush(
                CotizacionHistoricaEntity.desde(
                        CotizacionHistorica.calcular(
                                jugadorId,
                                semana,
                                new BigDecimal(valor),
                                OrigenCotizacion.AUTOMATICO,
                                fechaCalculo)));
    }

    private static BigDecimal valorDe(List<ValorPorJugadorProjection> filas, UUID jugadorId) {
        return filas.stream()
                .filter(f -> f.getJugadorId().equals(jugadorId))
                .findFirst()
                .orElseThrow()
                .getValor();
    }
}
