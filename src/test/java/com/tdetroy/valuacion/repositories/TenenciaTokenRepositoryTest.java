package com.tdetroy.valuacion.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tdetroy.valuacion.model.TenenciaToken;
import com.tdetroy.valuacion.repositories.support.PostgresIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Test de integración de {@link TenenciaTokenRepository} contra Postgres real (tasks.md T0.7,
 * constitution.md §1/§4). Igual que {@code UsuarioRepository}, {@link TenenciaToken} es mutable
 * después de creado — {@code acreditar}/{@code reservar}/etc. cambian {@code cantidad}/{@code
 * cantidadReservada} vía dirty checking de JPA — así que este test cubre además que esas
 * actualizaciones persisten, y que la constraint {@code uk_tenencias_token_usuario_jugador}
 * (V6__tenencias_token.sql, plan.md §2.5: "UNIQUE(usuarioId, jugadorId)") se cumple contra la base
 * real.
 */
class TenenciaTokenRepositoryTest extends PostgresIntegrationTest {

    @Autowired private TenenciaTokenRepository tenenciaTokenRepository;

    @Autowired private TestEntityManager entityManager;

    @Test
    void guardarTenenciaValida_persisteYSeLeeDeVueltaConLosValoresEsperados() {
        UUID usuarioId = UUID.randomUUID();
        UUID jugadorId = UUID.randomUUID();
        TenenciaToken tenencia = TenenciaToken.abrir(usuarioId, jugadorId);

        TenenciaToken guardada = tenenciaTokenRepository.saveAndFlush(tenencia);
        entityManager.clear();

        TenenciaToken leida = tenenciaTokenRepository.findById(guardada.getId()).orElseThrow();

        assertThat(leida.getId()).isEqualTo(tenencia.getId());
        assertThat(leida.getUsuarioId()).isEqualTo(usuarioId);
        assertThat(leida.getJugadorId()).isEqualTo(jugadorId);
        assertThat(leida.getCantidad()).isZero();
        assertThat(leida.getCantidadReservada()).isZero();
    }

    @Test
    void acreditarYReservarTrasGuardar_persistenElNuevoValorPorDirtyChecking() {
        TenenciaToken tenencia =
                tenenciaTokenRepository.saveAndFlush(
                        TenenciaToken.abrir(UUID.randomUUID(), UUID.randomUUID()));
        tenencia.acreditar(10);
        tenenciaTokenRepository.saveAndFlush(tenencia);
        entityManager.clear();

        TenenciaToken trasAcreditar =
                tenenciaTokenRepository.findById(tenencia.getId()).orElseThrow();
        assertThat(trasAcreditar.getCantidad()).isEqualTo(10);

        trasAcreditar.reservar(4);
        tenenciaTokenRepository.saveAndFlush(trasAcreditar);
        entityManager.clear();

        TenenciaToken trasReservar =
                tenenciaTokenRepository.findById(tenencia.getId()).orElseThrow();
        assertThat(trasReservar.getCantidad()).isEqualTo(10);
        assertThat(trasReservar.getCantidadReservada()).isEqualTo(4);
    }

    @Test
    void usuarioYJugadorDuplicados_violaConstraintUniqueDeLaBase() {
        UUID usuarioId = UUID.randomUUID();
        UUID jugadorId = UUID.randomUUID();
        tenenciaTokenRepository.saveAndFlush(TenenciaToken.abrir(usuarioId, jugadorId));

        assertThatThrownBy(
                        () ->
                                tenenciaTokenRepository.saveAndFlush(
                                        TenenciaToken.abrir(usuarioId, jugadorId)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
