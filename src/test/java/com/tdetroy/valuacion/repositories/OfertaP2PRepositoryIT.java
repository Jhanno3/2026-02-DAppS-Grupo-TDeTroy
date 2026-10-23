package com.tdetroy.valuacion.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import com.tdetroy.valuacion.entity.OfertaP2PEntity;
import com.tdetroy.valuacion.model.EstadoOferta;
import com.tdetroy.valuacion.model.OfertaP2P;
import com.tdetroy.valuacion.repositories.support.PostgresIntegrationTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * Test de integración de {@link OfertaP2PRepository} contra Postgres real (tasks.md T0.7, T6.1,
 * constitution.md §1/§4). {@link OfertaP2PEntity} es mutable después de creada — cambiar {@code
 * cantidadDisponible}/{@code estado} persiste vía dirty checking de JPA, mismo criterio que {@code
 * TenenciaTokenRepositoryIT}. {@link #findById} corre con {@code @Lock(PESSIMISTIC_WRITE)} (plan.md
 * §9.3) — acá sólo se cubre que la fila se lee igual que sin lock dentro de la transacción propia
 * del test ({@code @DataJpaTest} es transaccional); la concurrencia real entre compradores se
 * prueba en {@code OfertaServiceImplTest}/su IT (tasks.md T6.4), no acá.
 */
class OfertaP2PRepositoryIT extends PostgresIntegrationTest {

    @Autowired private OfertaP2PRepository ofertaP2PRepository;

    @Autowired private TestEntityManager entityManager;

    @Test
    void guardarOfertaValida_persisteYSeLeeDeVueltaConLosValoresEsperados() {
        UUID vendedorId = UUID.randomUUID();
        UUID jugadorId = UUID.randomUUID();
        Instant fechaCreacion = Instant.parse("2026-01-15T10:00:00Z");
        Instant fechaVencimiento = Instant.parse("2026-01-16T10:00:00Z");
        BigDecimal precioUnitario = new BigDecimal("50.00");
        OfertaP2P oferta =
                OfertaP2P.publicar(
                        vendedorId, jugadorId, 10, precioUnitario, fechaCreacion, fechaVencimiento);

        OfertaP2PEntity guardada = ofertaP2PRepository.saveAndFlush(OfertaP2PEntity.desde(oferta));
        entityManager.clear();

        OfertaP2P leida = ofertaP2PRepository.findById(guardada.getId()).orElseThrow().aModelo();

        assertThat(leida.getId()).isEqualTo(oferta.getId());
        assertThat(leida.getVendedorId()).isEqualTo(vendedorId);
        assertThat(leida.getJugadorId()).isEqualTo(jugadorId);
        assertThat(leida.getCantidadPublicada()).isEqualTo(10);
        assertThat(leida.getCantidadDisponible()).isEqualTo(10);
        assertThat(leida.getPrecioUnitario()).isEqualByComparingTo(precioUnitario);
        assertThat(leida.getEstado()).isEqualTo(EstadoOferta.ABIERTA);
        assertThat(leida.getFechaCreacion()).isEqualTo(fechaCreacion);
        assertThat(leida.getFechaVencimiento()).isEqualTo(fechaVencimiento);
    }

    @Test
    void actualizarCantidadDisponibleYEstadoTrasGuardar_persistenPorDirtyChecking() {
        OfertaP2PEntity oferta =
                ofertaP2PRepository.saveAndFlush(
                        OfertaP2PEntity.desde(
                                OfertaP2P.publicar(
                                        UUID.randomUUID(),
                                        UUID.randomUUID(),
                                        10,
                                        new BigDecimal("50.00"),
                                        24L)));

        oferta.setCantidadDisponible(0);
        oferta.setEstado(EstadoOferta.AGOTADA);
        ofertaP2PRepository.saveAndFlush(oferta);
        entityManager.clear();

        OfertaP2PEntity trasAgotarse = ofertaP2PRepository.findById(oferta.getId()).orElseThrow();
        assertThat(trasAgotarse.getCantidadDisponible()).isZero();
        assertThat(trasAgotarse.getEstado()).isEqualTo(EstadoOferta.AGOTADA);
    }
}
