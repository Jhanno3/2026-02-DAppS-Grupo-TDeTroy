package com.tdetroy.valuacion.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import com.tdetroy.valuacion.model.EstadoJugador;
import com.tdetroy.valuacion.model.Jugador;
import com.tdetroy.valuacion.repositories.support.PostgresIntegrationTest;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * Test de integración de {@link JugadorRepository} contra Postgres real (tasks.md T0.7,
 * constitution.md §1/§4). Igual que {@code UsuarioRepository}, {@link Jugador} es mutable después
 * de creado — {@code emitirTokens}/{@code liberarTokens}/{@code darDeBaja} cambian su estado vía
 * dirty checking de JPA, no reinsertando una fila — así que este test cubre además que esas
 * actualizaciones persisten.
 */
class JugadorRepositoryTest extends PostgresIntegrationTest {

    @Autowired private JugadorRepository jugadorRepository;

    @Autowired private TestEntityManager entityManager;

    @Test
    void guardarJugadorValido_persisteYSeLeeDeVueltaConLosValoresEsperados() {
        LocalDate fechaNacimiento = LocalDate.of(1987, 6, 24);
        Jugador jugador =
                Jugador.darAlta(
                        "Leonel Messi",
                        "Inter Miami CF",
                        "Delantero",
                        fechaNacimiento,
                        "Argentina");

        Jugador guardado = jugadorRepository.saveAndFlush(jugador);
        entityManager.clear();

        Jugador leido = jugadorRepository.findById(guardado.getId()).orElseThrow();

        assertThat(leido.getId()).isEqualTo(jugador.getId());
        assertThat(leido.getNombre()).isEqualTo("Leonel Messi");
        assertThat(leido.getClub()).isEqualTo("Inter Miami CF");
        assertThat(leido.getPosicion()).isEqualTo("Delantero");
        assertThat(leido.getFechaNacimiento()).isEqualTo(fechaNacimiento);
        assertThat(leido.getNacionalidad()).isEqualTo("Argentina");
        assertThat(leido.getEstado()).isEqualTo(EstadoJugador.ACTIVO);
        assertThat(leido.getTokensEmitidos()).isZero();
        assertThat(leido.getCotizacionVigenteId()).isNull();
        assertThat(leido.getFechaUltimaActualizacionRendimiento()).isNull();
    }

    @Test
    void emitirYLiberarTokensTrasGuardar_persistenElNuevoValorPorDirtyChecking() {
        Jugador jugador =
                jugadorRepository.saveAndFlush(
                        Jugador.darAlta(
                                "Kylian Mbappé",
                                "Real Madrid CF",
                                "Delantero",
                                LocalDate.of(1998, 12, 20),
                                "Francia"));
        jugador.emitirTokens(80);
        jugadorRepository.saveAndFlush(jugador);
        entityManager.clear();

        Jugador trasEmitir = jugadorRepository.findById(jugador.getId()).orElseThrow();
        assertThat(trasEmitir.getTokensEmitidos()).isEqualTo(80);

        trasEmitir.liberarTokens(30);
        jugadorRepository.saveAndFlush(trasEmitir);
        entityManager.clear();

        Jugador trasLiberar = jugadorRepository.findById(jugador.getId()).orElseThrow();
        assertThat(trasLiberar.getTokensEmitidos()).isEqualTo(50);
    }

    @Test
    void darDeBajaTrasGuardar_persisteEstadoInactivo() {
        Jugador jugador =
                jugadorRepository.saveAndFlush(
                        Jugador.darAlta(
                                "Jugador Retirado",
                                "Sin Club",
                                "Mediocampista",
                                LocalDate.of(1990, 1, 1),
                                "Uruguay"));
        jugador.darDeBaja();
        jugadorRepository.saveAndFlush(jugador);
        entityManager.clear();

        Jugador leido = jugadorRepository.findById(jugador.getId()).orElseThrow();
        assertThat(leido.getEstado()).isEqualTo(EstadoJugador.INACTIVO);
    }
}
