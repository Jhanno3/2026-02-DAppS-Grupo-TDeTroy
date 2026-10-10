package com.tdetroy.valuacion.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tdetroy.valuacion.entity.UsuarioEntity;
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
 * RegistroAuditoriaRepository} (append-only), {@link UsuarioEntity} es mutable después de creada —
 * cambiar {@code saldoVirtual} persiste vía dirty checking de JPA, no reinsertando una fila — así
 * que este test cubre además que esa actualización persiste. También cubre que la constraint {@code
 * uk_usuarios_email} (V4__usuarios.sql, plan.md §2.1: "email: String (único)") se cumple contra la
 * base real.
 */
class UsuarioRepositoryIT extends PostgresIntegrationTest {

    @Autowired private UsuarioRepository usuarioRepository;

    @Autowired private TestEntityManager entityManager;

    @Test
    void guardarUsuarioValido_persisteYSeLeeDeVueltaConLosValoresEsperados() {
        Instant fechaCreacion = Instant.parse("2026-01-15T10:00:00Z");
        Usuario usuario = Usuario.registrar("persona@example.com", "hash-bcrypt", fechaCreacion);

        UsuarioEntity guardado = usuarioRepository.saveAndFlush(UsuarioEntity.desde(usuario));
        entityManager.clear();

        Usuario leido = usuarioRepository.findById(guardado.getId()).orElseThrow().aModelo();

        assertThat(leido.getId()).isEqualTo(usuario.getId());
        assertThat(leido.getEmail()).isEqualTo("persona@example.com");
        assertThat(leido.getPasswordHash()).isEqualTo("hash-bcrypt");
        assertThat(leido.getRol()).isEqualTo(RolUsuario.USER);
        assertThat(leido.getSaldoVirtual()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(leido.getFechaCreacion()).isEqualTo(fechaCreacion);
    }

    @Test
    void actualizarSaldoTrasGuardar_persisteElNuevoValorPorDirtyChecking() {
        UsuarioEntity usuario =
                usuarioRepository.saveAndFlush(
                        UsuarioEntity.desde(Usuario.registrar("saldo@example.com", "hash-bcrypt")));
        usuario.setSaldoVirtual(new BigDecimal("100.00"));
        usuarioRepository.saveAndFlush(usuario);
        entityManager.clear();

        UsuarioEntity trasAcreditar = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertThat(trasAcreditar.getSaldoVirtual()).isEqualByComparingTo("100.00");

        trasAcreditar.setSaldoVirtual(new BigDecimal("60.00"));
        usuarioRepository.saveAndFlush(trasAcreditar);
        entityManager.clear();

        UsuarioEntity trasDebitar = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertThat(trasDebitar.getSaldoVirtual()).isEqualByComparingTo("60.00");
    }

    @Test
    void emailDuplicado_violaConstraintUniqueDeLaBase() {
        usuarioRepository.saveAndFlush(
                UsuarioEntity.desde(Usuario.registrar("duplicado@example.com", "hash-uno")));

        assertThatThrownBy(
                        () ->
                                usuarioRepository.saveAndFlush(
                                        UsuarioEntity.desde(
                                                Usuario.registrar(
                                                        "duplicado@example.com", "hash-dos"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
