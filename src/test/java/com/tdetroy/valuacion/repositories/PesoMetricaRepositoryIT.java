package com.tdetroy.valuacion.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import com.tdetroy.valuacion.entity.PesoMetricaEntity;
import com.tdetroy.valuacion.entity.PesoMetricaId;
import com.tdetroy.valuacion.model.PesoMetrica;
import com.tdetroy.valuacion.model.SentidoMetrica;
import com.tdetroy.valuacion.repositories.support.PostgresIntegrationTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/** Test de integración de {@link PesoMetricaRepository} contra Postgres real. */
class PesoMetricaRepositoryIT extends PostgresIntegrationTest {

    @Autowired private PesoMetricaRepository pesoMetricaRepository;

    @Autowired private TestEntityManager entityManager;

    @Test
    void guardarPesoMetricaValido_persisteYSeLeeDeVueltaConLosValoresEsperados() {
        PesoMetrica pesoMetrica =
                PesoMetrica.configurar(
                        "rating", new BigDecimal("1.5000"), true, SentidoMetrica.POSITIVO);

        pesoMetricaRepository.saveAndFlush(PesoMetricaEntity.desde(pesoMetrica));
        entityManager.clear();

        PesoMetrica leido =
                pesoMetricaRepository
                        .findById(new PesoMetricaId("rating", PesoMetrica.POSICION_GENERAL))
                        .orElseThrow()
                        .aModelo();

        assertThat(leido.getClave()).isEqualTo("rating");
        assertThat(leido.getPosicion()).isEqualTo(PesoMetrica.POSICION_GENERAL);
        assertThat(leido.getPeso()).isEqualByComparingTo(new BigDecimal("1.5000"));
        assertThat(leido.isActivo()).isTrue();
        assertThat(leido.getSentido()).isEqualTo(SentidoMetrica.POSITIVO);
    }

    @Test
    void guardarPesoMetricaPorPosicion_persisteYFiltraCorrectamente() {
        PesoMetrica general =
                PesoMetrica.configurar(
                        "goals",
                        "GENERAL",
                        new BigDecimal("0.5000"),
                        true,
                        SentidoMetrica.POSITIVO);
        PesoMetrica delantero =
                PesoMetrica.configurar(
                        "goals",
                        "Delantero",
                        new BigDecimal("0.5000"),
                        true,
                        SentidoMetrica.POSITIVO);
        PesoMetrica defensor =
                PesoMetrica.configurar(
                        "goals",
                        "Defensor",
                        new BigDecimal("1.5000"),
                        true,
                        SentidoMetrica.POSITIVO);

        pesoMetricaRepository.save(PesoMetricaEntity.desde(general));
        pesoMetricaRepository.save(PesoMetricaEntity.desde(delantero));
        pesoMetricaRepository.saveAndFlush(PesoMetricaEntity.desde(defensor));
        entityManager.clear();

        assertThat(pesoMetricaRepository.findByClave("goals")).hasSize(3);
        PesoMetrica leidoDefensor =
                pesoMetricaRepository
                        .findByClaveAndPosicion("goals", "Defensor")
                        .orElseThrow()
                        .aModelo();
        assertThat(leidoDefensor.getPeso()).isEqualByComparingTo(new BigDecimal("1.5000"));
    }

    @Test
    void actualizarPesoYEstadoActivo_persisteSobreLaMismaFila() {
        PesoMetrica pesoMetrica =
                PesoMetrica.configurar(
                        "asistencias", new BigDecimal("1.0000"), true, SentidoMetrica.POSITIVO);
        pesoMetricaRepository.saveAndFlush(PesoMetricaEntity.desde(pesoMetrica));
        entityManager.clear();

        pesoMetrica.actualizarPeso(new BigDecimal("1.2500"));
        pesoMetrica.desactivar();
        pesoMetricaRepository.saveAndFlush(PesoMetricaEntity.desde(pesoMetrica));
        entityManager.clear();

        PesoMetrica actualizado =
                pesoMetricaRepository
                        .findById(new PesoMetricaId("asistencias", PesoMetrica.POSICION_GENERAL))
                        .orElseThrow()
                        .aModelo();
        assertThat(actualizado.getPeso()).isEqualByComparingTo(new BigDecimal("1.2500"));
        assertThat(actualizado.isActivo()).isFalse();
    }
}
