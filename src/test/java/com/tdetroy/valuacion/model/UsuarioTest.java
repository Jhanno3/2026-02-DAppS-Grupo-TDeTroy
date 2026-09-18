package com.tdetroy.valuacion.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tdetroy.valuacion.common.exceptions.SaldoInsuficienteException;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Cubre la invariante propia de {@link Usuario} (plan.md §2.1): alta con saldo inicial cero (UC-01)
 * y los casos límite de {@code debitarSaldo}/{@code acreditarSaldo} exigidos por tasks.md T1.1 y
 * plan.md §12 (saldo exacto, saldo insuficiente, monto ≤0).
 */
class UsuarioTest {

    private static final String EMAIL = "persona@example.com";
    private static final String PASSWORD_HASH = "hash-bcrypt-de-prueba";
    private static final Instant FECHA_FIJA = Instant.parse("2026-01-01T00:00:00Z");

    // ---- Alta (UC-01) ----

    @Test
    void registrar_creaConRolUserYSaldoInicialCero() {
        Usuario usuario = Usuario.registrar(EMAIL, PASSWORD_HASH, FECHA_FIJA);

        assertThat(usuario.getId()).isNotNull();
        assertThat(usuario.getEmail()).isEqualTo(EMAIL);
        assertThat(usuario.getPasswordHash()).isEqualTo(PASSWORD_HASH);
        assertThat(usuario.getRol()).isEqualTo(RolUsuario.USER);
        assertThat(usuario.getSaldoVirtual()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(usuario.getFechaCreacion()).isEqualTo(FECHA_FIJA);
    }

    @Test
    void registrar_conEmailNulo_lanzaExcepcion() {
        assertThatNullPointerException()
                .isThrownBy(() -> Usuario.registrar(null, PASSWORD_HASH, FECHA_FIJA));
    }

    @Test
    void registrar_conEmailBlanco_lanzaExcepcion() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Usuario.registrar("   ", PASSWORD_HASH, FECHA_FIJA));
    }

    @Test
    void registrar_conPasswordHashBlanco_lanzaExcepcion() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Usuario.registrar(EMAIL, "", FECHA_FIJA));
    }

    @Test
    void registrarSinFechaExplicita_usaInstanteActual() {
        Instant antes = Instant.now();
        Usuario usuario = Usuario.registrar(EMAIL, PASSWORD_HASH);
        Instant despues = Instant.now();

        assertThat(usuario.getFechaCreacion()).isBetween(antes, despues);
    }

    @Test
    void dosUsuariosRegistradosPorSeparadoTienenIdsDistintos() {
        Usuario uno = Usuario.registrar(EMAIL, PASSWORD_HASH, FECHA_FIJA);
        Usuario otro = Usuario.registrar(EMAIL, PASSWORD_HASH, FECHA_FIJA);

        assertThat(uno.getId()).isNotEqualTo(otro.getId());
    }

    // ---- debitarSaldo ----

    @Test
    void debitarSaldo_conSaldoExacto_dejaSaldoEnCero() {
        Usuario usuario = Usuario.registrar(EMAIL, PASSWORD_HASH, FECHA_FIJA);
        usuario.acreditarSaldo(new BigDecimal("100.00"));

        usuario.debitarSaldo(new BigDecimal("100.00"));

        assertThat(usuario.getSaldoVirtual()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void debitarSaldo_conSaldoInsuficiente_lanzaSaldoInsuficienteException() {
        Usuario usuario = Usuario.registrar(EMAIL, PASSWORD_HASH, FECHA_FIJA);
        usuario.acreditarSaldo(new BigDecimal("50.00"));

        assertThatThrownBy(() -> usuario.debitarSaldo(new BigDecimal("50.01")))
                .isInstanceOf(SaldoInsuficienteException.class);
        assertThat(usuario.getSaldoVirtual()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void debitarSaldo_montoNulo_lanzaExcepcion() {
        Usuario usuario = Usuario.registrar(EMAIL, PASSWORD_HASH, FECHA_FIJA);

        assertThatNullPointerException().isThrownBy(() -> usuario.debitarSaldo(null));
    }

    @Test
    void debitarSaldo_montoCero_lanzaExcepcion() {
        Usuario usuario = Usuario.registrar(EMAIL, PASSWORD_HASH, FECHA_FIJA);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> usuario.debitarSaldo(BigDecimal.ZERO));
    }

    @Test
    void debitarSaldo_montoNegativo_lanzaExcepcion() {
        Usuario usuario = Usuario.registrar(EMAIL, PASSWORD_HASH, FECHA_FIJA);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> usuario.debitarSaldo(new BigDecimal("-0.01")));
    }

    // ---- acreditarSaldo ----

    @Test
    void acreditarSaldo_sumaAlSaldoExistente() {
        Usuario usuario = Usuario.registrar(EMAIL, PASSWORD_HASH, FECHA_FIJA);
        usuario.acreditarSaldo(new BigDecimal("30.00"));

        usuario.acreditarSaldo(new BigDecimal("20.00"));

        assertThat(usuario.getSaldoVirtual()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void acreditarSaldo_normalizaEscalaAMonetarioEscala() {
        Usuario usuario = Usuario.registrar(EMAIL, PASSWORD_HASH, FECHA_FIJA);

        usuario.acreditarSaldo(new BigDecimal("10.005"));

        assertThat(usuario.getSaldoVirtual().scale()).isEqualTo(2);
    }

    @Test
    void acreditarSaldo_montoNulo_lanzaExcepcion() {
        Usuario usuario = Usuario.registrar(EMAIL, PASSWORD_HASH, FECHA_FIJA);

        assertThatNullPointerException().isThrownBy(() -> usuario.acreditarSaldo(null));
    }

    @Test
    void acreditarSaldo_montoCero_lanzaExcepcion() {
        Usuario usuario = Usuario.registrar(EMAIL, PASSWORD_HASH, FECHA_FIJA);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> usuario.acreditarSaldo(BigDecimal.ZERO));
    }

    @Test
    void acreditarSaldo_montoNegativo_lanzaExcepcion() {
        Usuario usuario = Usuario.registrar(EMAIL, PASSWORD_HASH, FECHA_FIJA);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> usuario.acreditarSaldo(new BigDecimal("-5.00")));
    }
}
