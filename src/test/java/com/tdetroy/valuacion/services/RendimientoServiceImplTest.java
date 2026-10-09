package com.tdetroy.valuacion.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tdetroy.valuacion.entity.JugadorEntity;
import com.tdetroy.valuacion.entity.RendimientoPartidoEntity;
import com.tdetroy.valuacion.model.EstadoJugador;
import com.tdetroy.valuacion.model.FuenteResultado;
import com.tdetroy.valuacion.model.Jugador;
import com.tdetroy.valuacion.model.RendimientoPartido;
import com.tdetroy.valuacion.repositories.FixtureRepository;
import com.tdetroy.valuacion.repositories.JugadorRepository;
import com.tdetroy.valuacion.repositories.PartidoCrudo;
import com.tdetroy.valuacion.repositories.RendimientoCrudo;
import com.tdetroy.valuacion.repositories.RendimientoExternoRepository;
import com.tdetroy.valuacion.repositories.RendimientoPartidoRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

/**
 * Cubre {@link RendimientoServiceImpl} (tasks.md T3.5, UC-05): {@code ingestarFixtures} persiste un
 * {@code RendimientoPartido} con métricas vacías por cada jugador {@code ACTIVO} cuyo club disputó
 * un partido confirmado por Football-Data.org; {@code ingestarRendimiento} persiste uno por cada
 * partido devuelto por WhoScored y actualiza la fecha de última actualización de rendimiento del
 * jugador. Ambos aplican la clave de deduplicación (jugadorId + partidoExternoId) y aíslan la falla
 * de un jugador puntual sin interrumpir al resto del catálogo (constitution.md §1).
 */
@ExtendWith(MockitoExtension.class)
class RendimientoServiceImplTest {

    @Mock private FixtureRepository fixtureRepository;
    @Mock private RendimientoExternoRepository rendimientoExternoRepository;
    @Mock private RendimientoPartidoRepository rendimientoPartidoRepository;
    @Mock private JugadorRepository jugadorRepository;

    private RendimientoServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new RendimientoServiceImpl(
                        fixtureRepository,
                        rendimientoExternoRepository,
                        rendimientoPartidoRepository,
                        jugadorRepository,
                        new ObjectMapper());
    }

    private static Jugador jugadorActivo(String club) {
        return Jugador.darAlta(
                "Jugador de prueba", club, "Delantero", LocalDate.of(2000, 1, 1), "Argentina");
    }

    // ---- ingestarFixtures ----

    @Test
    void ingestarFixtures_sinPartidosDisputados_noConsultaJugadoresNiPersisteNada() {
        when(fixtureRepository.obtenerPartidosDisputados(any(), any())).thenReturn(List.of());

        service.ingestarFixtures();

        verifyNoInteractions(jugadorRepository, rendimientoPartidoRepository);
    }

    @Test
    void ingestarFixtures_conJugadorActivoDelClubQueJugo_persisteRendimientoConMetricasVacias() {
        Jugador jugador = jugadorActivo("Club A");
        PartidoCrudo partido =
                new PartidoCrudo(
                        "111", LocalDate.of(2026, 2, 15), "FINISHED", "Club A", "Club B", 2, 1);
        when(fixtureRepository.obtenerPartidosDisputados(any(), any()))
                .thenReturn(List.of(partido));
        when(jugadorRepository.findByEstado(EstadoJugador.ACTIVO))
                .thenReturn(List.of(JugadorEntity.desde(jugador)));
        when(rendimientoPartidoRepository.existsByJugadorIdAndPartidoExternoId(any(), any()))
                .thenReturn(false);

        service.ingestarFixtures();

        RendimientoPartido guardado = capturarGuardado();
        assertThat(guardado.getJugadorId()).isEqualTo(jugador.getId());
        assertThat(guardado.getPartidoExternoId()).isEqualTo("111");
        assertThat(guardado.getFechaPartido()).isEqualTo(LocalDate.of(2026, 2, 15));
        assertThat(guardado.getSemanaCalculo()).isEqualTo("2026-W07");
        assertThat(guardado.getMetricas()).isEqualTo("{}");
        assertThat(guardado.getFuenteResultado()).isEqualTo(FuenteResultado.FOOTBALL_DATA);
        verify(jugadorRepository, never()).save(any());
    }

    @Test
    void ingestarFixtures_conJugadorDeClubQueNoJugoElPartido_noPersisteNadaParaEse() {
        Jugador jugador = jugadorActivo("Club C");
        PartidoCrudo partido =
                new PartidoCrudo(
                        "111", LocalDate.of(2026, 2, 15), "FINISHED", "Club A", "Club B", 2, 1);
        when(fixtureRepository.obtenerPartidosDisputados(any(), any()))
                .thenReturn(List.of(partido));
        when(jugadorRepository.findByEstado(EstadoJugador.ACTIVO))
                .thenReturn(List.of(JugadorEntity.desde(jugador)));

        service.ingestarFixtures();

        verify(rendimientoPartidoRepository, never()).save(any());
    }

    @Test
    void ingestarFixtures_conFilaYaIngerida_noVuelveAPersistir() {
        Jugador jugador = jugadorActivo("Club A");
        PartidoCrudo partido =
                new PartidoCrudo(
                        "111", LocalDate.of(2026, 2, 15), "FINISHED", "Club A", "Club B", 2, 1);
        when(fixtureRepository.obtenerPartidosDisputados(any(), any()))
                .thenReturn(List.of(partido));
        when(jugadorRepository.findByEstado(EstadoJugador.ACTIVO))
                .thenReturn(List.of(JugadorEntity.desde(jugador)));
        when(rendimientoPartidoRepository.existsByJugadorIdAndPartidoExternoId(
                        jugador.getId(), "111"))
                .thenReturn(true);

        service.ingestarFixtures();

        verify(rendimientoPartidoRepository, never()).save(any());
    }

    @Test
    void ingestarFixtures_fallaAlPersistirUnJugador_noInterrumpeLaIngestaDeLosDemas() {
        Jugador fallido = jugadorActivo("Club A");
        Jugador exitoso = jugadorActivo("Club A");
        PartidoCrudo partido =
                new PartidoCrudo(
                        "111", LocalDate.of(2026, 2, 15), "FINISHED", "Club A", "Club B", 2, 1);
        when(fixtureRepository.obtenerPartidosDisputados(any(), any()))
                .thenReturn(List.of(partido));
        when(jugadorRepository.findByEstado(EstadoJugador.ACTIVO))
                .thenReturn(List.of(JugadorEntity.desde(fallido), JugadorEntity.desde(exitoso)));
        when(rendimientoPartidoRepository.existsByJugadorIdAndPartidoExternoId(
                        fallido.getId(), "111"))
                .thenThrow(new RuntimeException("fallo inesperado"));
        when(rendimientoPartidoRepository.existsByJugadorIdAndPartidoExternoId(
                        exitoso.getId(), "111"))
                .thenReturn(false);

        service.ingestarFixtures();

        verify(rendimientoPartidoRepository).save(any());
    }

    // ---- ingestarRendimiento ----

    @Test
    void ingestarRendimiento_sinJugadoresActivos_noConsultaWhoScored() {
        when(jugadorRepository.findByEstado(EstadoJugador.ACTIVO)).thenReturn(List.of());

        service.ingestarRendimiento();

        verifyNoInteractions(rendimientoExternoRepository, rendimientoPartidoRepository);
    }

    @Test
    void ingestarRendimiento_conDatosNuevos_persisteYActualizaFechaDelJugador() {
        Jugador jugador = jugadorActivo("Club A");
        when(jugadorRepository.findByEstado(EstadoJugador.ACTIVO))
                .thenReturn(List.of(JugadorEntity.desde(jugador)));
        RendimientoCrudo crudo =
                new RendimientoCrudo("222", LocalDate.of(2026, 2, 15), Map.of("rating", 7.8));
        when(rendimientoExternoRepository.obtenerRendimiento(eq(jugador.getId()), any(), any()))
                .thenReturn(List.of(crudo));
        when(rendimientoPartidoRepository.existsByJugadorIdAndPartidoExternoId(
                        jugador.getId(), "222"))
                .thenReturn(false);

        service.ingestarRendimiento();

        RendimientoPartido guardado = capturarGuardado();
        assertThat(guardado.getMetricas()).contains("\"rating\":7.8");
        assertThat(guardado.getFuenteResultado()).isEqualTo(FuenteResultado.FOOTBALL_DATA);

        ArgumentCaptor<JugadorEntity> jugadorCaptor = ArgumentCaptor.forClass(JugadorEntity.class);
        verify(jugadorRepository).save(jugadorCaptor.capture());
        assertThat(jugadorCaptor.getValue().getFechaUltimaActualizacionRendimiento()).isNotNull();
    }

    @Test
    void ingestarRendimiento_conFilaYaIngerida_noPersisteNiActualizaFechaDelJugador() {
        Jugador jugador = jugadorActivo("Club A");
        when(jugadorRepository.findByEstado(EstadoJugador.ACTIVO))
                .thenReturn(List.of(JugadorEntity.desde(jugador)));
        RendimientoCrudo crudo =
                new RendimientoCrudo("222", LocalDate.of(2026, 2, 15), Map.of("rating", 7.8));
        when(rendimientoExternoRepository.obtenerRendimiento(eq(jugador.getId()), any(), any()))
                .thenReturn(List.of(crudo));
        when(rendimientoPartidoRepository.existsByJugadorIdAndPartidoExternoId(
                        jugador.getId(), "222"))
                .thenReturn(true);

        service.ingestarRendimiento();

        verify(rendimientoPartidoRepository, never()).save(any());
        verify(jugadorRepository, never()).save(any());
    }

    @Test
    void ingestarRendimiento_fallaAlConsultarWhoScoredDeUnJugador_noInterrumpeALosDemas() {
        Jugador fallido = jugadorActivo("Club A");
        Jugador exitoso = jugadorActivo("Club B");
        when(jugadorRepository.findByEstado(EstadoJugador.ACTIVO))
                .thenReturn(List.of(JugadorEntity.desde(fallido), JugadorEntity.desde(exitoso)));
        when(rendimientoExternoRepository.obtenerRendimiento(eq(fallido.getId()), any(), any()))
                .thenThrow(new RuntimeException("whoscored no disponible"));
        RendimientoCrudo crudo =
                new RendimientoCrudo("333", LocalDate.of(2026, 2, 15), Map.of("goles", 1));
        when(rendimientoExternoRepository.obtenerRendimiento(eq(exitoso.getId()), any(), any()))
                .thenReturn(List.of(crudo));
        when(rendimientoPartidoRepository.existsByJugadorIdAndPartidoExternoId(
                        exitoso.getId(), "333"))
                .thenReturn(false);

        service.ingestarRendimiento();

        verify(rendimientoPartidoRepository).save(any());
        verify(jugadorRepository).save(any());
    }

    private RendimientoPartido capturarGuardado() {
        ArgumentCaptor<RendimientoPartidoEntity> captor =
                ArgumentCaptor.forClass(RendimientoPartidoEntity.class);
        verify(rendimientoPartidoRepository).save(captor.capture());
        return captor.getValue().aModelo();
    }
}
