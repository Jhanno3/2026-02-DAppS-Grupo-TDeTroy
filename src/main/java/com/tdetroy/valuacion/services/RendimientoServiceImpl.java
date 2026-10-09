package com.tdetroy.valuacion.services;

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
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * {@inheritDoc}
 *
 * <p>La ventana de ingesta de ambos métodos es siempre "de este lunes a hoy" (semana ISO en curso,
 * la misma que calculará {@code CotizacionServiceImpl} el lunes siguiente): ambos corren el domingo
 * previo al cierre de esa semana (tasks.md T3.6, plan.md §8.2).
 *
 * <p>{@code jugadorId} + {@code partidoExternoId} es la clave de deduplicación (migración V7,
 * {@code RendimientoPartidoRepository#existsByJugadorIdAndPartidoExternoId}): una reingesta del
 * mismo ciclo semanal (ej. reintento manual tras una falla parcial) nunca duplica una fila ya
 * persistida.
 *
 * <p>Cada jugador se procesa de forma aislada (constitution.md §1 "manejo explícito de
 * fallos/indisponibilidad"): una excepción al traducir/persistir el rendimiento de un jugador
 * puntual se loguea y no interrumpe la ingesta del resto del catálogo.
 */
@Service
public class RendimientoServiceImpl implements RendimientoService {

    private static final Logger log = LoggerFactory.getLogger(RendimientoServiceImpl.class);

    private static final String METRICAS_SIN_DATOS = "{}";

    private final FixtureRepository fixtureRepository;
    private final RendimientoExternoRepository rendimientoExternoRepository;
    private final RendimientoPartidoRepository rendimientoPartidoRepository;
    private final JugadorRepository jugadorRepository;
    private final ObjectMapper objectMapper;

    public RendimientoServiceImpl(
            FixtureRepository fixtureRepository,
            RendimientoExternoRepository rendimientoExternoRepository,
            RendimientoPartidoRepository rendimientoPartidoRepository,
            JugadorRepository jugadorRepository,
            ObjectMapper objectMapper) {
        this.fixtureRepository = fixtureRepository;
        this.rendimientoExternoRepository = rendimientoExternoRepository;
        this.rendimientoPartidoRepository = rendimientoPartidoRepository;
        this.jugadorRepository = jugadorRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void ingestarFixtures() {
        LocalDate hasta = LocalDate.now();
        LocalDate desde = hasta.with(DayOfWeek.MONDAY);

        List<PartidoCrudo> partidos = fixtureRepository.obtenerPartidosDisputados(desde, hasta);
        if (partidos.isEmpty()) {
            return;
        }

        List<Jugador> jugadoresActivos = jugadoresActivos();
        for (PartidoCrudo partido : partidos) {
            for (Jugador jugador : jugadoresActivos) {
                if (clubDisputoElPartido(jugador, partido)) {
                    ingestarPartidoDeJugador(jugador, partido);
                }
            }
        }
    }

    @Override
    @Transactional
    public void ingestarRendimiento() {
        LocalDate hasta = LocalDate.now();
        LocalDate desde = hasta.with(DayOfWeek.MONDAY);

        for (Jugador jugador : jugadoresActivos()) {
            ingestarRendimientoDeJugador(jugador, desde, hasta);
        }
    }

    private void ingestarPartidoDeJugador(Jugador jugador, PartidoCrudo partido) {
        try {
            ingestarSiNoExiste(
                    jugador.getId(),
                    partido.partidoExternoId(),
                    partido.fecha(),
                    METRICAS_SIN_DATOS);
        } catch (RuntimeException ex) {
            log.error(
                    "Fallo al ingerir el fixture {} del jugador {}: {}",
                    partido.partidoExternoId(),
                    jugador.getId(),
                    ex.getMessage(),
                    ex);
        }
    }

    private void ingestarRendimientoDeJugador(Jugador jugador, LocalDate desde, LocalDate hasta) {
        List<RendimientoCrudo> rendimientos;
        try {
            rendimientos =
                    rendimientoExternoRepository.obtenerRendimiento(jugador.getId(), desde, hasta);
        } catch (RuntimeException ex) {
            log.error(
                    "Fallo al obtener rendimiento de WhoScored para el jugador {}: {}",
                    jugador.getId(),
                    ex.getMessage(),
                    ex);
            return;
        }

        boolean huboIngestaNueva = false;
        for (RendimientoCrudo rendimiento : rendimientos) {
            try {
                String metricas = objectMapper.writeValueAsString(rendimiento.metricas());
                boolean insertado =
                        ingestarSiNoExiste(
                                jugador.getId(),
                                rendimiento.partidoExternoId(),
                                rendimiento.fechaPartido(),
                                metricas);
                huboIngestaNueva = huboIngestaNueva || insertado;
            } catch (RuntimeException ex) {
                log.error(
                        "Fallo al ingerir el rendimiento del partido {} del jugador {}: {}",
                        rendimiento.partidoExternoId(),
                        jugador.getId(),
                        ex.getMessage(),
                        ex);
            }
        }

        if (huboIngestaNueva) {
            jugador.registrarActualizacionRendimiento(Instant.now());
            jugadorRepository.save(JugadorEntity.desde(jugador));
        }
    }

    private boolean ingestarSiNoExiste(
            UUID jugadorId, String partidoExternoId, LocalDate fechaPartido, String metricas) {
        if (rendimientoPartidoRepository.existsByJugadorIdAndPartidoExternoId(
                jugadorId, partidoExternoId)) {
            return false;
        }
        String semanaCalculo = semanaIsoDe(fechaPartido);
        rendimientoPartidoRepository.save(
                RendimientoPartidoEntity.desde(
                        RendimientoPartido.ingestar(
                                jugadorId,
                                partidoExternoId,
                                fechaPartido,
                                semanaCalculo,
                                metricas,
                                FuenteResultado.FOOTBALL_DATA)));
        return true;
    }

    private List<Jugador> jugadoresActivos() {
        return jugadorRepository.findByEstado(EstadoJugador.ACTIVO).stream()
                .map(JugadorEntity::aModelo)
                .toList();
    }

    private static boolean clubDisputoElPartido(Jugador jugador, PartidoCrudo partido) {
        return jugador.getClub().equalsIgnoreCase(partido.equipoLocal())
                || jugador.getClub().equalsIgnoreCase(partido.equipoVisitante());
    }

    private static String semanaIsoDe(LocalDate fecha) {
        int semana = fecha.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int anio = fecha.get(IsoFields.WEEK_BASED_YEAR);
        return "%04d-W%02d".formatted(anio, semana);
    }
}
