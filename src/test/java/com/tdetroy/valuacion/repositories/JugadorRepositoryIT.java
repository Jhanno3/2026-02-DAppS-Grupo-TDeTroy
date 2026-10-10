package com.tdetroy.valuacion.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import com.tdetroy.valuacion.entity.JugadorEntity;
import com.tdetroy.valuacion.model.EstadoJugador;
import com.tdetroy.valuacion.model.Jugador;
import com.tdetroy.valuacion.repositories.support.PostgresIntegrationTest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * Test de integración de {@link JugadorRepository} contra Postgres real (tasks.md T0.7,
 * constitution.md §1/§4). {@link JugadorEntity} es mutable después de creada — cambiar sus setters
 * (ej. {@code tokensEmitidos}, {@code estado}) persiste vía dirty checking de JPA, no reinsertando
 * una fila — así que este test cubre además que esas actualizaciones persisten.
 */
class JugadorRepositoryIT extends PostgresIntegrationTest {

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

        JugadorEntity guardado = jugadorRepository.saveAndFlush(JugadorEntity.desde(jugador));
        entityManager.clear();

        Jugador leido = jugadorRepository.findById(guardado.getId()).orElseThrow().aModelo();

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
    void actualizarCamposMutablesTrasGuardar_persistenElNuevoValorPorDirtyChecking() {
        JugadorEntity jugador =
                jugadorRepository.saveAndFlush(
                        JugadorEntity.desde(
                                Jugador.darAlta(
                                        "Kylian Mbappé",
                                        "Real Madrid CF",
                                        "Delantero",
                                        LocalDate.of(1998, 12, 20),
                                        "Francia")));
        jugador.setTokensEmitidos(80);
        jugadorRepository.saveAndFlush(jugador);
        entityManager.clear();

        JugadorEntity trasEmitir = jugadorRepository.findById(jugador.getId()).orElseThrow();
        assertThat(trasEmitir.getTokensEmitidos()).isEqualTo(80);

        trasEmitir.setTokensEmitidos(50);
        trasEmitir.setEstado(EstadoJugador.INACTIVO);
        jugadorRepository.saveAndFlush(trasEmitir);
        entityManager.clear();

        JugadorEntity trasActualizar = jugadorRepository.findById(jugador.getId()).orElseThrow();
        assertThat(trasActualizar.getTokensEmitidos()).isEqualTo(50);
        assertThat(trasActualizar.getEstado()).isEqualTo(EstadoJugador.INACTIVO);
    }

    @Test
    void findByEstado_devuelveSoloLosJugadoresEnEseEstado() {
        JugadorEntity activo =
                jugadorRepository.saveAndFlush(
                        JugadorEntity.desde(
                                Jugador.darAlta(
                                        "Jugador Activo",
                                        "Club A",
                                        "Delantero",
                                        LocalDate.of(2000, 1, 1),
                                        "Argentina")));
        JugadorEntity inactivo =
                JugadorEntity.desde(
                        Jugador.darAlta(
                                "Jugador Inactivo",
                                "Club B",
                                "Arquero",
                                LocalDate.of(1995, 5, 5),
                                "Brasil"));
        inactivo.setEstado(EstadoJugador.INACTIVO);
        jugadorRepository.saveAndFlush(inactivo);
        entityManager.clear();

        List<JugadorEntity> activos = jugadorRepository.findByEstado(EstadoJugador.ACTIVO);

        assertThat(activos).extracting(JugadorEntity::getId).containsExactly(activo.getId());
    }
}
