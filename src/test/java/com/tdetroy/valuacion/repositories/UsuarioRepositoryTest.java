package com.tdetroy.valuacion.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tdetroy.valuacion.model.RolUsuario;
import com.tdetroy.valuacion.model.Usuario;
import com.tdetroy.valuacion.repositories.support.PostgresIntegrationTest;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Test de integración de {@link UsuarioRepository} contra Postgres real (tasks.md T0.7,
 * constitution.md §1/§4). A diferencia de {@code MovimientoRepository}/{@code
 * RegistroAuditoriaRepository} (append-only), {@link Usuario} es mutable después de creado — {@code
 * debitarSaldo}/{@code acreditarSaldo} cambian {@code saldoVirtual} vía dirty checking de JPA, no
 * reinsertando una fila — así que este test cubre además que esa actualización persiste. También
 * cubre que la constraint {@code uk_usuarios_email} (V4__usuarios.sql, plan.md §2.1: "email: String
 * (único)") se cumple contra la base real.
 */
class UsuarioRepositoryTest extends PostgresIntegrationTest {

    @Autowired private UsuarioRepository usuarioRepository;

    @Autowired private TestEntityManager entityManager;

    @Test
    void guardarUsuarioValido_persisteYSeLeeDeVueltaConLosValoresEsperados() {
        Instant fechaCreacion = Instant.parse("2026-01-15T10:00:00Z");
        Usuario usuario = Usuario.registrar("persona@example.com", "hash-bcrypt", fechaCreacion);

        Usuario guardado = usuarioRepository.saveAndFlush(usuario);
        entityManager.clear();

        Usuario leido = usuarioRepository.findById(guardado.getId()).orElseThrow();

        assertThat(leido.getId()).isEqualTo(usuario.getId());
        assertThat(leido.getEmail()).isEqualTo("persona@example.com");
        assertThat(leido.getPasswordHash()).isEqualTo("hash-bcrypt");
        assertThat(leido.getRol()).isEqualTo(RolUsuario.USER);
        assertThat(leido.getSaldoVirtual()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(leido.getFechaCreacion()).isEqualTo(fechaCreacion);
    }

    @Test
    void debitarYAcreditarSaldoTrasGuardar_persistenElNuevoValorPorDirtyChecking() {
        Usuario usuario =
                usuarioRepository.saveAndFlush(
                        Usuario.registrar("saldo@example.com", "hash-bcrypt"));
        usuario.acreditarSaldo(new BigDecimal("100.00"));
        usuarioRepository.saveAndFlush(usuario);
        entityManager.clear();

        Usuario trasAcreditar = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertThat(trasAcreditar.getSaldoVirtual()).isEqualByComparingTo("100.00");

        trasAcreditar.debitarSaldo(new BigDecimal("40.00"));
        usuarioRepository.saveAndFlush(trasAcreditar);
        entityManager.clear();

        Usuario trasDebitar = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertThat(trasDebitar.getSaldoVirtual()).isEqualByComparingTo("60.00");
    }

    @Test
    void emailDuplicado_violaConstraintUniqueDeLaBase() {
        usuarioRepository.saveAndFlush(Usuario.registrar("duplicado@example.com", "hash-uno"));

        assertThatThrownBy(
                        () ->
                                usuarioRepository.saveAndFlush(
                                        Usuario.registrar("duplicado@example.com", "hash-dos")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
