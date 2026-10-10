package com.tdetroy.valuacion.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import com.tdetroy.valuacion.entity.PesoMetricaEntity;
import com.tdetroy.valuacion.model.PesoMetrica;
import com.tdetroy.valuacion.repositories.support.PostgresIntegrationTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * Test de integración de {@link PesoMetricaRepository} contra Postgres real (tasks.md T4.2,
 * constitution.md §1/§4): persistir un {@link PesoMetrica} válido vía {@link
 * PesoMetrica#configurar} y leerlo de vuelta con los mismos valores, y que actualizar el
 * peso/estado activo de una fila ya persistida (plan.md §6.2: "ajustable en runtime vía tabla")
 * efectivamente actualiza esa misma fila (vía {@code merge}, {@code clave} ya no nula) en vez de
 * insertar una nueva — a diferencia de {@link RendimientoPartidoRepository}/{@link
 * CotizacionHistoricaRepository}, acá {@code clave} es una clave natural, no un {@code UUID}
 * generado en cada alta, así que guardar dos veces la misma clave es una actualización legítima, no
 * un conflicto.
 */
class PesoMetricaRepositoryIT extends PostgresIntegrationTest {

    @Autowired private PesoMetricaRepository pesoMetricaRepository;

    @Autowired private TestEntityManager entityManager;

    @Test
    void guardarPesoMetricaValido_persisteYSeLeeDeVueltaConLosValoresEsperados() {
        PesoMetrica pesoMetrica = PesoMetrica.configurar("rating", new BigDecimal("1.5000"), true);

        pesoMetricaRepository.saveAndFlush(PesoMetricaEntity.desde(pesoMetrica));
        entityManager.clear();

        PesoMetrica leido = pesoMetricaRepository.findById("rating").orElseThrow().aModelo();

        assertThat(leido.getClave()).isEqualTo("rating");
        assertThat(leido.getPeso()).isEqualByComparingTo(new BigDecimal("1.5000"));
        assertThat(leido.isActivo()).isTrue();
    }

    @Test
    void actualizarPesoYEstadoActivo_persisteSobreLaMismaFila() {
        PesoMetrica pesoMetrica =
                PesoMetrica.configurar("asistencias", new BigDecimal("1.0000"), true);
        pesoMetricaRepository.saveAndFlush(PesoMetricaEntity.desde(pesoMetrica));
        entityManager.clear();

        pesoMetrica.actualizarPeso(new BigDecimal("1.2500"));
        pesoMetrica.desactivar();
        pesoMetricaRepository.saveAndFlush(PesoMetricaEntity.desde(pesoMetrica));
        entityManager.clear();

        assertThat(pesoMetricaRepository.count()).isEqualTo(1);
        PesoMetrica actualizado =
                pesoMetricaRepository.findById("asistencias").orElseThrow().aModelo();
        assertThat(actualizado.getPeso()).isEqualByComparingTo(new BigDecimal("1.2500"));
        assertThat(actualizado.isActivo()).isFalse();
    }
}
