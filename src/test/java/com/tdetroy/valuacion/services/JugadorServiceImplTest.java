package com.tdetroy.valuacion.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException;
import com.tdetroy.valuacion.model.EstadoJugador;
import com.tdetroy.valuacion.model.Jugador;
import com.tdetroy.valuacion.repositories.JugadorRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Cubre {@link JugadorServiceImpl}: {@code listar}/{@code obtenerPorId} (UC-07), {@code darAlta}
 * (UC-03) y {@code editar} (UC-04, nunca toca cotización ni límite de tokens).
 */
@ExtendWith(MockitoExtension.class)
class JugadorServiceImplTest {

    private static final String NOMBRE = "Leonel Messi";
    private static final String CLUB = "Inter Miami CF";
    private static final String POSICION = "Delantero";
    private static final LocalDate FECHA_NACIMIENTO = LocalDate.of(1987, 6, 24);
    private static final String NACIONALIDAD = "Argentina";

    @Mock private JugadorRepository jugadorRepository;
    @Mock private AuditoriaService auditoriaService;

    private JugadorServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new JugadorServiceImpl(jugadorRepository, auditoriaService);
    }

    // ---- listar / obtenerPorId (UC-07) ----

    @Test
    void listar_delegaEnElRepositorioYDevuelveElCatalogoCompleto() {
        Jugador jugador = Jugador.darAlta(NOMBRE, CLUB, POSICION, FECHA_NACIMIENTO, NACIONALIDAD);
        when(jugadorRepository.findAll()).thenReturn(List.of(jugador));

        assertThat(service.listar()).containsExactly(jugador);
    }

    @Test
    void obtenerPorId_conJugadorExistente_loDevuelve() {
        UUID jugadorId = UUID.randomUUID();
        Jugador jugador = Jugador.darAlta(NOMBRE, CLUB, POSICION, FECHA_NACIMIENTO, NACIONALIDAD);
        when(jugadorRepository.findById(jugadorId)).thenReturn(Optional.of(jugador));

        assertThat(service.obtenerPorId(jugadorId)).isEqualTo(jugador);
    }

    @Test
    void obtenerPorId_conJugadorInexistente_lanzaExcepcion() {
        UUID jugadorId = UUID.randomUUID();
        when(jugadorRepository.findById(jugadorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorId(jugadorId))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining(jugadorId.toString());
    }

    // ---- darAlta (UC-03) ----

    @Test
    void darAlta_conDatosValidos_guardaAuditaYRetornaElJugadorActivo() {
        UUID actorId = UUID.randomUUID();
        when(jugadorRepository.save(any(Jugador.class))).thenAnswer(inv -> inv.getArgument(0));

        Jugador creado =
                service.darAlta(NOMBRE, CLUB, POSICION, FECHA_NACIMIENTO, NACIONALIDAD, actorId);

        assertThat(creado.getNombre()).isEqualTo(NOMBRE);
        assertThat(creado.getEstado()).isEqualTo(EstadoJugador.ACTIVO);
        assertThat(creado.getTokensEmitidos()).isZero();
        verify(jugadorRepository).save(any(Jugador.class));
        verify(auditoriaService)
                .registrar(
                        eq(actorId),
                        eq("ALTA_JUGADOR"),
                        eq("Jugador"),
                        eq(creado.getId()),
                        isNull(),
                        any());
    }

    @Test
    void darAlta_conNombreBlanco_lanzaExcepcionYNoGuardaNiAudita() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                service.darAlta(
                                        "   ",
                                        CLUB,
                                        POSICION,
                                        FECHA_NACIMIENTO,
                                        NACIONALIDAD,
                                        UUID.randomUUID()));

        verifyNoInteractions(jugadorRepository, auditoriaService);
    }

    // ---- editar (UC-04) ----

    @Test
    void editar_conJugadorExistente_actualizaDatosAuditaYNuncaTocaEstadoNiTokens() {
        UUID jugadorId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Jugador jugador =
                Jugador.darAlta(
                        "Nombre Viejo", "Club Viejo", "Arquero", FECHA_NACIMIENTO, "Brasil");
        jugador.emitirTokens(40);
        when(jugadorRepository.findById(jugadorId)).thenReturn(Optional.of(jugador));

        Jugador editado =
                service.editar(
                        jugadorId, NOMBRE, CLUB, POSICION, FECHA_NACIMIENTO, NACIONALIDAD, actorId);

        assertThat(editado.getNombre()).isEqualTo(NOMBRE);
        assertThat(editado.getClub()).isEqualTo(CLUB);
        assertThat(editado.getPosicion()).isEqualTo(POSICION);
        assertThat(editado.getNacionalidad()).isEqualTo(NACIONALIDAD);
        assertThat(editado.getEstado()).isEqualTo(EstadoJugador.ACTIVO);
        assertThat(editado.getTokensEmitidos()).isEqualTo(40);
        assertThat(editado.getCotizacionVigenteId()).isNull();

        verify(auditoriaService)
                .registrar(
                        eq(actorId),
                        eq("EDICION_JUGADOR"),
                        eq("Jugador"),
                        eq(jugadorId),
                        any(),
                        any());
    }

    @Test
    void editar_conJugadorInexistente_lanzaExcepcionYNoAudita() {
        UUID jugadorId = UUID.randomUUID();
        when(jugadorRepository.findById(jugadorId)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.editar(
                                        jugadorId,
                                        NOMBRE,
                                        CLUB,
                                        POSICION,
                                        FECHA_NACIMIENTO,
                                        NACIONALIDAD,
                                        UUID.randomUUID()))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining(jugadorId.toString());

        verifyNoInteractions(auditoriaService);
    }

    @Test
    void editar_conClubBlanco_lanzaExcepcionYNoAuditaNiMutaElJugador() {
        UUID jugadorId = UUID.randomUUID();
        Jugador jugador = Jugador.darAlta(NOMBRE, CLUB, POSICION, FECHA_NACIMIENTO, NACIONALIDAD);
        when(jugadorRepository.findById(jugadorId)).thenReturn(Optional.of(jugador));

        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                service.editar(
                                        jugadorId,
                                        NOMBRE,
                                        "   ",
                                        POSICION,
                                        FECHA_NACIMIENTO,
                                        NACIONALIDAD,
                                        UUID.randomUUID()));

        assertThat(jugador.getClub()).isEqualTo(CLUB);
        verify(jugadorRepository, never()).save(any());
        verifyNoInteractions(auditoriaService);
    }
}
