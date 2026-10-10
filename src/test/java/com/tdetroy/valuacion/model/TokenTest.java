package com.tdetroy.valuacion.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tdetroy.valuacion.common.exceptions.EmisionMaximaSuperadaException;
import org.junit.jupiter.api.Test;

/**
 * Cubre la invariante propia de {@link Token} (plan.md §2.2, constitution.md §2): máximo 100 tokens
 * emitidos bajo ninguna operación — directamente sobre {@link Token}, independiente de {@link
 * JugadorTest} (que ya la ejercita indirectamente a través de la API pública de {@link Jugador}).
 */
class TokenTest {

    @Test
    void nueva_empiezaEnCero() {
        Token token = Token.nueva();

        assertThat(token.getCantidadEmitida()).isZero();
        assertThat(token.disponibles()).isEqualTo(100);
    }

    @Test
    void emitir_sumaALaCantidadEmitida() {
        Token token = Token.nueva();

        token.emitir(40);
        token.emitir(30);

        assertThat(token.getCantidadEmitida()).isEqualTo(70);
        assertThat(token.disponibles()).isEqualTo(30);
    }

    @Test
    void emitir_hastaExactamenteCien_permitido() {
        Token token = Token.nueva();

        token.emitir(100);

        assertThat(token.getCantidadEmitida()).isEqualTo(100);
        assertThat(token.disponibles()).isZero();
    }

    @Test
    void emitir_queSupereCien_lanzaEmisionMaximaSuperadaExceptionYNoMutaEstado() {
        Token token = Token.nueva();
        token.emitir(90);

        assertThatThrownBy(() -> token.emitir(11))
                .isInstanceOf(EmisionMaximaSuperadaException.class);
        assertThat(token.getCantidadEmitida()).isEqualTo(90);
    }

    @Test
    void emitir_cantidadCero_lanzaExcepcion() {
        Token token = Token.nueva();

        assertThatIllegalArgumentException().isThrownBy(() -> token.emitir(0));
    }

    @Test
    void emitir_cantidadNegativa_lanzaExcepcion() {
        Token token = Token.nueva();

        assertThatIllegalArgumentException().isThrownBy(() -> token.emitir(-1));
    }

    @Test
    void liberar_restaDeLaCantidadEmitida() {
        Token token = Token.nueva();
        token.emitir(50);

        token.liberar(20);

        assertThat(token.getCantidadEmitida()).isEqualTo(30);
    }

    @Test
    void liberar_todoLoEmitido_dejaLaCantidadEmitidaEnCero() {
        Token token = Token.nueva();
        token.emitir(50);

        token.liberar(50);

        assertThat(token.getCantidadEmitida()).isZero();
    }

    @Test
    void liberar_queSupereLoEmitido_lanzaExcepcionYNoMutaEstado() {
        Token token = Token.nueva();
        token.emitir(10);

        assertThatIllegalArgumentException().isThrownBy(() -> token.liberar(11));
        assertThat(token.getCantidadEmitida()).isEqualTo(10);
    }

    @Test
    void liberar_cantidadCero_lanzaExcepcion() {
        Token token = Token.nueva();
        token.emitir(10);

        assertThatIllegalArgumentException().isThrownBy(() -> token.liberar(0));
    }

    @Test
    void liberar_cantidadNegativa_lanzaExcepcion() {
        Token token = Token.nueva();
        token.emitir(10);

        assertThatIllegalArgumentException().isThrownBy(() -> token.liberar(-1));
    }

    @Test
    void reconstruir_conservaLaCantidadEmitidaSinRevalidarComoSeLlego() {
        Token token = Token.reconstruir(65);

        assertThat(token.getCantidadEmitida()).isEqualTo(65);
        assertThat(token.disponibles()).isEqualTo(35);
    }
}
