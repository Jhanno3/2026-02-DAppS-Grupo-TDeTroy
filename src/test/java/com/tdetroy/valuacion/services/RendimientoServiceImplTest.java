package com.tdetroy.valuacion.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tdetroy.valuacion.common.SemanaIso;
import com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException;
import com.tdetroy.valuacion.entity.JugadorEntity;
import com.tdetroy.valuacion.entity.RendimientoAcumuladoWhoScoredEntity;
import com.tdetroy.valuacion.entity.RendimientoPartidoEntity;
import com.tdetroy.valuacion.model.EstadisticasTemporada;
import com.tdetroy.valuacion.model.EstadoJugador;
import com.tdetroy.valuacion.model.FuenteResultado;
import com.tdetroy.valuacion.model.Jugador;
import com.tdetroy.valuacion.model.RendimientoAcumuladoWhoScored;
import com.tdetroy.valuacion.model.RendimientoPartido;
import com.tdetroy.valuacion.repositories.FixtureRepository;
import com.tdetroy.valuacion.repositories.JugadorRendimientoLigaCrudo;
import com.tdetroy.valuacion.repositories.JugadorRepository;
import com.tdetroy.valuacion.repositories.PartidoCrudo;
import com.tdetroy.valuacion.repositories.RendimientoAcumuladoWhoScoredRepository;
import com.tdetroy.valuacion.repositories.RendimientoLigaExternoRepository;
import com.tdetroy.valuacion.repositories.RendimientoPartidoRepository;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Cubre {@link RendimientoServiceImpl} (UC-05): {@code ingestarFixtures} persiste un {@code
 * RendimientoPartido} con métricas vacías por cada jugador {@code ACTIVO} cuyo club disputó un
 * partido confirmado por Football-Data.org; {@code ingestarRendimiento} refresca la foto de cada
 * jugador {@code ACTIVO} y, por cada liga presente en el catálogo, resuelve el acumulado de
 * WhoScored por nombre+club y persiste el delta semanal contra el snapshot previo. Ambos aplican la
 * clave de deduplicación (jugadorId + partidoExternoId) y aíslan la falla de un jugador/liga
 * puntual sin interrumpir al resto del catálogo (constitution.md §1).
 */
@ExtendWith(MockitoExtension.class)
class RendimientoServiceImplTest {

    @Mock private FixtureRepository fixtureRepository;
    @Mock private RendimientoLigaExternoRepository rendimientoLigaExternoRepository;
    @Mock private RendimientoPartidoRepository rendimientoPartidoRepository;
    @Mock private RendimientoAcumuladoWhoScoredRepository rendimientoAcumuladoWhoScoredRepository;
    @Mock private JugadorRepository jugadorRepository;

    private RendimientoServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new RendimientoServiceImpl(
                        fixtureRepository,
                        rendimientoLigaExternoRepository,
                        rendimientoPartidoRepository,
                        rendimientoAcumuladoWhoScoredRepository,
                        jugadorRepository,
                        new ObjectMapper());
    }

    private static Jugador jugadorActivo(String club) {
        return jugador("Jugador de prueba", club, "Liga de prueba");
    }

    private static Jugador jugador(String nombre, String club, String liga) {
        return Jugador.darAlta(
                nombre, club, "Delantero", LocalDate.of(2000, 1, 1), "Argentina", liga);
    }

    // ---- ingestarFixtures ----

    @Test
    void ingestarFixtures_sinPartidosDisputados_noConsultaJugadoresNiPersisteNada() {
        when(fixtureRepository.obtenerPartidosDisputados(any(), any())).thenReturn(List.of());

        service.ingestarFixtures();

        verifyNoInteractions(jugadorRepository, rendimientoPartidoRepository);
    }

    @Test
    void ingestarFixtures_consultaLaSemanaDeReferenciaDeLunesADomingo() {
        when(fixtureRepository.obtenerPartidosDisputados(any(), any())).thenReturn(List.of());
        LocalDate referencia = SemanaIso.fechaDeReferencia(LocalDate.now());

        service.ingestarFixtures();

        verify(fixtureRepository)
                .obtenerPartidosDisputados(referencia.with(DayOfWeek.MONDAY), referencia);
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

        RendimientoPartido guardado = capturarRendimientoPartidoGuardado();
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

    // ---- ingestarRendimiento: foto de perfil ----

    @Test
    void ingestarRendimiento_sinJugadoresActivos_noConsultaNadaMas() {
        when(jugadorRepository.findByEstado(EstadoJugador.ACTIVO)).thenReturn(List.of());

        service.ingestarRendimiento();

        verifyNoInteractions(
                rendimientoLigaExternoRepository,
                rendimientoPartidoRepository,
                rendimientoAcumuladoWhoScoredRepository);
    }

    @Test
    void ingestarRendimiento_filaConFotoNueva_actualizaLaFotoAunqueNoHayaDelta() {
        Jugador jugador = jugadorActivo("Club A");
        when(jugadorRepository.findByEstado(EstadoJugador.ACTIVO))
                .thenReturn(List.of(JugadorEntity.desde(jugador)));
        when(rendimientoLigaExternoRepository.obtenerAcumuladoLiga("Liga de prueba"))
                .thenReturn(
                        List.of(
                                new JugadorRendimientoLigaCrudo(
                                        jugador.getNombre(),
                                        jugador.getClub(),
                                        Map.of("goals", 5),
                                        "https://whoscored.test/img/players/1.jpg")));
        when(rendimientoAcumuladoWhoScoredRepository.findById(jugador.getId()))
                .thenReturn(
                        Optional.of(
                                RendimientoAcumuladoWhoScoredEntity.desde(
                                        RendimientoAcumuladoWhoScored.primeraVez(
                                                jugador.getId(), "{\"goals\":5}"))));

        service.ingestarRendimiento();

        ArgumentCaptor<JugadorEntity> jugadorCaptor = ArgumentCaptor.forClass(JugadorEntity.class);
        verify(jugadorRepository).save(jugadorCaptor.capture());
        assertThat(jugadorCaptor.getValue().getFotoUrl())
                .isEqualTo("https://whoscored.test/img/players/1.jpg");
        verify(rendimientoPartidoRepository, never()).save(any());
    }

    @Test
    void ingestarRendimiento_filaConLaMismaFotoYSinDelta_noGuardaAlJugador() {
        Jugador jugador = jugadorActivo("Club A");
        jugador.actualizarFoto("https://whoscored.test/img/players/1.jpg");
        when(jugadorRepository.findByEstado(EstadoJugador.ACTIVO))
                .thenReturn(List.of(JugadorEntity.desde(jugador)));
        when(rendimientoLigaExternoRepository.obtenerAcumuladoLiga("Liga de prueba"))
                .thenReturn(
                        List.of(
                                new JugadorRendimientoLigaCrudo(
                                        jugador.getNombre(),
                                        jugador.getClub(),
                                        Map.of("goals", 5),
                                        "https://whoscored.test/img/players/1.jpg")));
        when(rendimientoAcumuladoWhoScoredRepository.findById(jugador.getId()))
                .thenReturn(
                        Optional.of(
                                RendimientoAcumuladoWhoScoredEntity.desde(
                                        RendimientoAcumuladoWhoScored.primeraVez(
                                                jugador.getId(), "{\"goals\":5}"))));

        service.ingestarRendimiento();

        verify(jugadorRepository, never()).save(any());
    }

    // ---- ingestarRendimiento: acumulado por liga ----

    @Test
    void
            ingestarRendimiento_primeraVezParaUnJugador_soloGuardaSnapshotSinPersistirRendimientoPartido() {
        Jugador jugador = jugadorActivo("Club A");
        when(jugadorRepository.findByEstado(EstadoJugador.ACTIVO))
                .thenReturn(List.of(JugadorEntity.desde(jugador)));
        when(rendimientoLigaExternoRepository.obtenerAcumuladoLiga("Liga de prueba"))
                .thenReturn(
                        List.of(
                                new JugadorRendimientoLigaCrudo(
                                        jugador.getNombre(),
                                        jugador.getClub(),
                                        Map.of("goals", 5, "assists", 2),
                                        null)));
        when(rendimientoAcumuladoWhoScoredRepository.findById(jugador.getId()))
                .thenReturn(Optional.empty());

        service.ingestarRendimiento();

        ArgumentCaptor<RendimientoAcumuladoWhoScoredEntity> captor =
                ArgumentCaptor.forClass(RendimientoAcumuladoWhoScoredEntity.class);
        verify(rendimientoAcumuladoWhoScoredRepository).save(captor.capture());
        assertThat(metricasComoMapa(captor.getValue().getMetricas()))
                .containsEntry("goals", 5)
                .containsEntry("assists", 2);
        verify(rendimientoPartidoRepository, never()).save(any());
        verify(jugadorRepository, never()).save(any());
    }

    @Test
    void
            ingestarRendimiento_conSnapshotPrevioYDeltaPositivo_persisteRendimientoPartidoConElDelta() {
        Jugador jugador = jugadorActivo("Club A");
        when(jugadorRepository.findByEstado(EstadoJugador.ACTIVO))
                .thenReturn(List.of(JugadorEntity.desde(jugador)));
        when(rendimientoLigaExternoRepository.obtenerAcumuladoLiga("Liga de prueba"))
                .thenReturn(
                        List.of(
                                new JugadorRendimientoLigaCrudo(
                                        jugador.getNombre(),
                                        jugador.getClub(),
                                        Map.of("goals", 8, "assists", 2),
                                        null)));
        RendimientoAcumuladoWhoScored snapshotPrevio =
                RendimientoAcumuladoWhoScored.primeraVez(
                        jugador.getId(), "{\"goals\":5,\"assists\":2}");
        when(rendimientoAcumuladoWhoScoredRepository.findById(jugador.getId()))
                .thenReturn(Optional.of(RendimientoAcumuladoWhoScoredEntity.desde(snapshotPrevio)));
        when(rendimientoPartidoRepository.existsByJugadorIdAndPartidoExternoId(any(), any()))
                .thenReturn(false);

        service.ingestarRendimiento();

        RendimientoPartido guardado = capturarRendimientoPartidoGuardado();
        assertThat(guardado.getJugadorId()).isEqualTo(jugador.getId());
        assertThat(guardado.getPartidoExternoId()).startsWith("WHOSCORED-LIGA-");
        assertThat(guardado.getFuenteResultado()).isEqualTo(FuenteResultado.WHOSCORED_LIGA);
        assertThat(metricasComoMapa(guardado.getMetricas()))
                .containsEntry("goals", 3)
                .containsEntry("assists", 0);

        ArgumentCaptor<RendimientoAcumuladoWhoScoredEntity> snapshotCaptor =
                ArgumentCaptor.forClass(RendimientoAcumuladoWhoScoredEntity.class);
        verify(rendimientoAcumuladoWhoScoredRepository).save(snapshotCaptor.capture());
        assertThat(metricasComoMapa(snapshotCaptor.getValue().getMetricas()))
                .containsEntry("goals", 8)
                .containsEntry("assists", 2);

        verify(jugadorRepository).save(any());
    }

    @Test
    void ingestarRendimiento_contadorMenorAlSnapshotPrevio_elDeltaDeEseCampoEsCero() {
        Jugador jugador = jugadorActivo("Club A");
        when(jugadorRepository.findByEstado(EstadoJugador.ACTIVO))
                .thenReturn(List.of(JugadorEntity.desde(jugador)));
        when(rendimientoLigaExternoRepository.obtenerAcumuladoLiga("Liga de prueba"))
                .thenReturn(
                        List.of(
                                new JugadorRendimientoLigaCrudo(
                                        jugador.getNombre(),
                                        jugador.getClub(),
                                        Map.of("goals", 3, "assists", 5),
                                        null)));
        RendimientoAcumuladoWhoScored snapshotPrevio =
                RendimientoAcumuladoWhoScored.primeraVez(
                        jugador.getId(), "{\"goals\":5,\"assists\":2}");
        when(rendimientoAcumuladoWhoScoredRepository.findById(jugador.getId()))
                .thenReturn(Optional.of(RendimientoAcumuladoWhoScoredEntity.desde(snapshotPrevio)));
        when(rendimientoPartidoRepository.existsByJugadorIdAndPartidoExternoId(any(), any()))
                .thenReturn(false);

        service.ingestarRendimiento();

        RendimientoPartido guardado = capturarRendimientoPartidoGuardado();
        assertThat(metricasComoMapa(guardado.getMetricas()))
                .containsEntry("goals", 0)
                .containsEntry("assists", 3);
    }

    @Test
    void
            ingestarRendimiento_sinCambiosRespectoDelSnapshot_actualizaSnapshotPeroNoPersisteRendimientoPartido() {
        Jugador jugador = jugadorActivo("Club A");
        when(jugadorRepository.findByEstado(EstadoJugador.ACTIVO))
                .thenReturn(List.of(JugadorEntity.desde(jugador)));
        when(rendimientoLigaExternoRepository.obtenerAcumuladoLiga("Liga de prueba"))
                .thenReturn(
                        List.of(
                                new JugadorRendimientoLigaCrudo(
                                        jugador.getNombre(),
                                        jugador.getClub(),
                                        Map.of("goals", 5),
                                        null)));
        RendimientoAcumuladoWhoScored snapshotPrevio =
                RendimientoAcumuladoWhoScored.primeraVez(jugador.getId(), "{\"goals\":5}");
        when(rendimientoAcumuladoWhoScoredRepository.findById(jugador.getId()))
                .thenReturn(Optional.of(RendimientoAcumuladoWhoScoredEntity.desde(snapshotPrevio)));

        service.ingestarRendimiento();

        verify(rendimientoAcumuladoWhoScoredRepository).save(any());
        verify(rendimientoPartidoRepository, never()).save(any());
        verify(jugadorRepository, never()).save(any());
    }

    @Test
    void ingestarRendimiento_metricaNoPermitida_seFiltraYNoLlegaAlSnapshot() {
        Jugador jugador = jugadorActivo("Club A");
        when(jugadorRepository.findByEstado(EstadoJugador.ACTIVO))
                .thenReturn(List.of(JugadorEntity.desde(jugador)));
        when(rendimientoLigaExternoRepository.obtenerAcumuladoLiga("Liga de prueba"))
                .thenReturn(
                        List.of(
                                new JugadorRendimientoLigaCrudo(
                                        jugador.getNombre(),
                                        jugador.getClub(),
                                        Map.of("goals", 5, "rating", 7.8),
                                        null)));
        when(rendimientoAcumuladoWhoScoredRepository.findById(jugador.getId()))
                .thenReturn(Optional.empty());

        service.ingestarRendimiento();

        ArgumentCaptor<RendimientoAcumuladoWhoScoredEntity> captor =
                ArgumentCaptor.forClass(RendimientoAcumuladoWhoScoredEntity.class);
        verify(rendimientoAcumuladoWhoScoredRepository).save(captor.capture());
        assertThat(metricasComoMapa(captor.getValue().getMetricas())).doesNotContainKey("rating");
    }

    @Test
    void ingestarRendimiento_sinMatchDeNombreClub_seIgnoraLaFilaYNoPersisteNada() {
        Jugador jugador = jugadorActivo("Club A");
        when(jugadorRepository.findByEstado(EstadoJugador.ACTIVO))
                .thenReturn(List.of(JugadorEntity.desde(jugador)));
        when(rendimientoLigaExternoRepository.obtenerAcumuladoLiga("Liga de prueba"))
                .thenReturn(
                        List.of(
                                new JugadorRendimientoLigaCrudo(
                                        "Alguien Completamente Distinto",
                                        "Otro Club",
                                        Map.of("goals", 5),
                                        null)));

        service.ingestarRendimiento();

        verifyNoInteractions(rendimientoAcumuladoWhoScoredRepository);
        verify(rendimientoPartidoRepository, never()).save(any());
    }

    @Test
    void ingestarRendimiento_registraElDeltaEnLaSemanaDeReferenciaConUnIdPorCorrida() {
        Jugador jugador = jugadorActivo("Club A");
        when(jugadorRepository.findByEstado(EstadoJugador.ACTIVO))
                .thenReturn(List.of(JugadorEntity.desde(jugador)));
        when(rendimientoLigaExternoRepository.obtenerAcumuladoLiga("Liga de prueba"))
                .thenReturn(
                        List.of(
                                new JugadorRendimientoLigaCrudo(
                                        jugador.getNombre(),
                                        jugador.getClub(),
                                        Map.of("goals", 8),
                                        null)));
        RendimientoAcumuladoWhoScored snapshotPrevio =
                RendimientoAcumuladoWhoScored.primeraVez(jugador.getId(), "{\"goals\":5}");
        when(rendimientoAcumuladoWhoScoredRepository.findById(jugador.getId()))
                .thenReturn(Optional.of(RendimientoAcumuladoWhoScoredEntity.desde(snapshotPrevio)));
        LocalDate referencia = SemanaIso.fechaDeReferencia(LocalDate.now());
        String idSemanal = "WHOSCORED-LIGA-" + SemanaIso.de(referencia);

        service.ingestarRendimiento();

        RendimientoPartido guardado = capturarRendimientoPartidoGuardado();
        assertThat(guardado.getFechaPartido()).isEqualTo(referencia);
        assertThat(guardado.getSemanaCalculo()).isEqualTo(SemanaIso.de(referencia));
        // Un id fijo por semana descartaba la segunda corrida de la semana después de pisar el
        // snapshot; con uno por corrida, ese delta se suma en vez de perderse.
        assertThat(guardado.getPartidoExternoId()).startsWith(idSemanal + "-");
        assertThat(metricasComoMapa(guardado.getMetricas())).containsEntry("goals", 3);
    }

    @Test
    void ingestarRendimiento_fallaAlObtenerUnaLiga_noInterrumpeLaIngestaDeOtrasLigas() {
        Jugador deLigaConFalla = jugador("Jugador A", "Club A", "Liga con falla");
        Jugador deOtraLiga = jugador("Jugador B", "Club B", "Otra liga");
        when(jugadorRepository.findByEstado(EstadoJugador.ACTIVO))
                .thenReturn(
                        List.of(
                                JugadorEntity.desde(deLigaConFalla),
                                JugadorEntity.desde(deOtraLiga)));
        when(rendimientoLigaExternoRepository.obtenerAcumuladoLiga("Liga con falla"))
                .thenThrow(new RuntimeException("whoscored no disponible"));
        when(rendimientoLigaExternoRepository.obtenerAcumuladoLiga("Otra liga"))
                .thenReturn(
                        List.of(
                                new JugadorRendimientoLigaCrudo(
                                        deOtraLiga.getNombre(),
                                        deOtraLiga.getClub(),
                                        Map.of("goals", 4),
                                        null)));
        when(rendimientoAcumuladoWhoScoredRepository.findById(deOtraLiga.getId()))
                .thenReturn(Optional.empty());

        service.ingestarRendimiento();

        verify(rendimientoAcumuladoWhoScoredRepository).save(any());
    }

    private RendimientoPartido capturarRendimientoPartidoGuardado() {
        ArgumentCaptor<RendimientoPartidoEntity> captor =
                ArgumentCaptor.forClass(RendimientoPartidoEntity.class);
        verify(rendimientoPartidoRepository).save(captor.capture());
        return captor.getValue().aModelo();
    }

    // ---- obtenerEstadisticasTemporada (UC-07) ----

    @Test
    void obtenerEstadisticasTemporada_conJugadorInexistente_lanzaExcepcion() {
        UUID jugadorId = UUID.randomUUID();
        when(jugadorRepository.existsById(jugadorId)).thenReturn(false);

        assertThatThrownBy(() -> service.obtenerEstadisticasTemporada(jugadorId))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void obtenerEstadisticasTemporada_sinSnapshot_devuelveVacio() {
        UUID jugadorId = UUID.randomUUID();
        when(jugadorRepository.existsById(jugadorId)).thenReturn(true);
        when(rendimientoAcumuladoWhoScoredRepository.findById(jugadorId))
                .thenReturn(Optional.empty());

        assertThat(service.obtenerEstadisticasTemporada(jugadorId)).isEmpty();
    }

    @Test
    void obtenerEstadisticasTemporada_conSnapshot_mapeaLasMetricasYCompletaConCeroLasQueFaltan() {
        UUID jugadorId = UUID.randomUUID();
        Instant fecha = Instant.parse("2026-10-29T19:43:00Z");
        when(jugadorRepository.existsById(jugadorId)).thenReturn(true);
        // Sin "redCard": una métrica ausente del snapshot se informa como 0.
        when(rendimientoAcumuladoWhoScoredRepository.findById(jugadorId))
                .thenReturn(
                        Optional.of(
                                RendimientoAcumuladoWhoScoredEntity.desde(
                                        RendimientoAcumuladoWhoScored.primeraVez(
                                                jugadorId,
                                                """
                                                {"apps":9,"minsPlayed":503,"goals":2,"assists":3,\
                                                "manOfTheMatch":0,"yellowCard":1}""",
                                                fecha))));

        EstadisticasTemporada estadisticas =
                service.obtenerEstadisticasTemporada(jugadorId).orElseThrow();

        assertThat(estadisticas).isEqualTo(new EstadisticasTemporada(9, 503, 2, 3, 0, 1, 0, fecha));
    }

    private static Map<String, Object> metricasComoMapa(String json) {
        try {
            return new ObjectMapper().readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (RuntimeException ex) {
            throw new AssertionError("JSON invalido: " + json, ex);
        }
    }
}
