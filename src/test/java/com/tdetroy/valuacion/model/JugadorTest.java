package com.tdetroy.valuacion.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tdetroy.valuacion.common.exceptions.EmisionMaximaSuperadaException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Cubre la invariante propia de {@link Jugador} (plan.md §2.2, constitution.md §2): máximo 100
 * tokens emitidos bajo ninguna operación, y los casos límite exigidos por tasks.md T2.1/plan.md §12
 * (emisión = 100 exacto, intento de exceder, baja).
 */
class JugadorTest {

    private static final String NOMBRE = "Leonel Messi";
    private static final String CLUB = "Inter Miami CF";
    private static final String POSICION = "Delantero";
    private static final LocalDate FECHA_NACIMIENTO = LocalDate.of(1987, 6, 24);
    private static final String NACIONALIDAD = "Argentina";

    private static Jugador jugadorValido() {
        return Jugador.darAlta(NOMBRE, CLUB, POSICION, FECHA_NACIMIENTO, NACIONALIDAD);
    }

    // ---- Alta (UC-03) ----

    @Test
    void darAlta_creaActivoConCeroTokensEmitidosYSinCotizacion() {
        Jugador jugador = jugadorValido();

        assertThat(jugador.getId()).isNotNull();
        assertThat(jugador.getNombre()).isEqualTo(NOMBRE);
        assertThat(jugador.getClub()).isEqualTo(CLUB);
        assertThat(jugador.getPosicion()).isEqualTo(POSICION);
        assertThat(jugador.getFechaNacimiento()).isEqualTo(FECHA_NACIMIENTO);
        assertThat(jugador.getNacionalidad()).isEqualTo(NACIONALIDAD);
        assertThat(jugador.getEstado()).isEqualTo(EstadoJugador.ACTIVO);
        assertThat(jugador.getTokensEmitidos()).isZero();
        assertThat(jugador.getCotizacionVigenteId()).isNull();
        assertThat(jugador.getFechaUltimaActualizacionRendimiento()).isNull();
    }

    @Test
    void darAlta_conNombreNulo_lanzaExcepcion() {
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                Jugador.darAlta(
                                        null, CLUB, POSICION, FECHA_NACIMIENTO, NACIONALIDAD));
    }

    @Test
    void darAlta_conClubBlanco_lanzaExcepcion() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                Jugador.darAlta(
                                        NOMBRE, "   ", POSICION, FECHA_NACIMIENTO, NACIONALIDAD));
    }

    @Test
    void darAlta_conFechaNacimientoNula_lanzaExcepcion() {
        assertThatNullPointerException()
                .isThrownBy(() -> Jugador.darAlta(NOMBRE, CLUB, POSICION, null, NACIONALIDAD));
    }

    @Test
    void dosJugadoresDadosDeAltaPorSeparadoTienenIdsDistintos() {
        Jugador uno = jugadorValido();
        Jugador otro = jugadorValido();

        assertThat(uno.getId()).isNotEqualTo(otro.getId());
    }

    // ---- emitirTokens ----

    @Test
    void emitirTokens_sumaALosYaEmitidos() {
        Jugador jugador = jugadorValido();

        jugador.emitirTokens(40);
        jugador.emitirTokens(30);

        assertThat(jugador.getTokensEmitidos()).isEqualTo(70);
    }

    @Test
    void emitirTokens_hastaExactamenteCien_permitido() {
        Jugador jugador = jugadorValido();

        jugador.emitirTokens(100);

        assertThat(jugador.getTokensEmitidos()).isEqualTo(100);
    }

    @Test
    void emitirTokens_queSupereCien_lanzaEmisionMaximaSuperadaExceptionYNoMutaEstado() {
        Jugador jugador = jugadorValido();
        jugador.emitirTokens(90);

        assertThatThrownBy(() -> jugador.emitirTokens(11))
                .isInstanceOf(EmisionMaximaSuperadaException.class);
        assertThat(jugador.getTokensEmitidos()).isEqualTo(90);
    }

    @Test
    void emitirTokens_cantidadCero_lanzaExcepcion() {
        Jugador jugador = jugadorValido();

        assertThatIllegalArgumentException().isThrownBy(() -> jugador.emitirTokens(0));
    }

    @Test
    void emitirTokens_cantidadNegativa_lanzaExcepcion() {
        Jugador jugador = jugadorValido();

        assertThatIllegalArgumentException().isThrownBy(() -> jugador.emitirTokens(-1));
    }

    // ---- liberarTokens ----

    @Test
    void liberarTokens_restaDeLosEmitidos() {
        Jugador jugador = jugadorValido();
        jugador.emitirTokens(50);

        jugador.liberarTokens(20);

        assertThat(jugador.getTokensEmitidos()).isEqualTo(30);
    }

    @Test
    void liberarTokens_todoLoEmitido_dejaTokensEmitidosEnCero() {
        Jugador jugador = jugadorValido();
        jugador.emitirTokens(50);

        jugador.liberarTokens(50);

        assertThat(jugador.getTokensEmitidos()).isZero();
    }

    @Test
    void liberarTokens_queSupereLoEmitido_lanzaExcepcion() {
        Jugador jugador = jugadorValido();
        jugador.emitirTokens(10);

        assertThatIllegalArgumentException().isThrownBy(() -> jugador.liberarTokens(11));
        assertThat(jugador.getTokensEmitidos()).isEqualTo(10);
    }

    @Test
    void liberarTokens_cantidadCero_lanzaExcepcion() {
        Jugador jugador = jugadorValido();
        jugador.emitirTokens(10);

        assertThatIllegalArgumentException().isThrownBy(() -> jugador.liberarTokens(0));
    }

    // ---- darDeBaja (UC-15) ----

    @Test
    void darDeBaja_pasaEstadoAInactivo() {
        Jugador jugador = jugadorValido();

        jugador.darDeBaja();

        assertThat(jugador.getEstado()).isEqualTo(EstadoJugador.INACTIVO);
    }

    @Test
    void darDeBaja_dejaTokensEmitidosCongeladosComoRegistroHistorico() {
        Jugador jugador = jugadorValido();
        jugador.emitirTokens(65);

        jugador.darDeBaja();

        assertThat(jugador.getTokensEmitidos()).isEqualTo(65);
    }

    // ---- tokensDisponibles (UC-07) ----

    @Test
    void tokensDisponibles_recienDadoDeAlta_esCien() {
        Jugador jugador = jugadorValido();

        assertThat(jugador.tokensDisponibles()).isEqualTo(100);
    }

    @Test
    void tokensDisponibles_conAlgunosEmitidos_esCienMenosLoEmitido() {
        Jugador jugador = jugadorValido();
        jugador.emitirTokens(30);

        assertThat(jugador.tokensDisponibles()).isEqualTo(70);
    }

    @Test
    void tokensDisponibles_jugadorInactivo_esSiempreCeroAunqueQuedenTokensSinEmitir() {
        Jugador jugador = jugadorValido();
        jugador.emitirTokens(40);

        jugador.darDeBaja();

        assertThat(jugador.tokensDisponibles()).isZero();
    }
}
