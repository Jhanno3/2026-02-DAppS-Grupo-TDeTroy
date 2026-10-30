package com.tdetroy.valuacion.services;

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
import com.tdetroy.valuacion.repositories.RendimientoExternoRepository;
import com.tdetroy.valuacion.repositories.RendimientoLigaExternoRepository;
import com.tdetroy.valuacion.repositories.RendimientoPartidoRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * {@inheritDoc}
 *
 * <p>La ventana de ingesta de {@code ingestarFixtures} es siempre "de este lunes a hoy" (semana ISO
 * en curso, la misma que calculará {@code CotizacionServiceImpl} el lunes siguiente): corre el
 * domingo previo al cierre de esa semana, igual que {@code ingestarRendimiento} (tasks.md T3.6,
 * plan.md §8.2).
 *
 * <p><b>Ingesta de rendimiento por liga (plan.md §8.1):</b> {@code ingestarRendimiento} agrupa a
 * los jugadores {@code ACTIVO} por {@code liga}, pide a {@link RendimientoLigaExternoRepository} el
 * acumulado de temporada de toda esa liga en una sola llamada (no una por jugador), y resuelve cada
 * fila cruda a un jugador del catálogo por nombre+club (ver {@link NombreClubMatcher}) antes de
 * procesarla — una fila sin un match inequívoco se ignora (se loguea, no bloquea al resto de la
 * liga). Por cada jugador resuelto:
 *
 * <ul>
 *   <li>Si no existe un {@code RendimientoAcumuladoWhoScored} previo, se persiste el acumulado
 *       recibido como snapshot base y no se genera ningún {@code RendimientoPartido} — todavía no
 *       hay un punto de comparación para calcular un delta.
 *   <li>Si ya existe, se calcula el delta por métrica (actual - anterior, nunca negativo — un
 *       contador que bajó respecto del snapshot anterior aporta 0, nunca resta) contra ese
 *       snapshot, se reemplaza el snapshot por el nuevo acumulado, y — sólo si al menos una métrica
 *       tuvo delta positivo — se persiste ese delta como el {@code RendimientoPartido} de la semana
 *       en curso ({@code fuenteResultado = WHOSCORED_LIGA}, {@code partidoExternoId} sintético
 *       {@code "WHOSCORED-LIGA-{semana}"}, deduplicando frente a una reingesta del mismo ciclo
 *       igual que el resto de las fuentes). Un delta íntegramente en cero (sin partidos nuevos esa
 *       semana) deliberadamente no persiste fila: así el jugador cae en el mismo camino de "sin
 *       rendimiento esta semana" (decaimiento leve, plan.md §6.3) que ya maneja {@code
 *       CotizacionServiceImpl}, en vez de aportar un puntaje neutro distinto.
 * </ul>
 *
 * <p>También aprovecha el mismo ciclo semanal para refrescar la foto de perfil de cada jugador vía
 * {@code RendimientoExternoRepository#obtenerFotoUrl} (plan.md §8.1) — no es rendimiento deportivo,
 * pero sale de la misma fuente y comparte la misma cadencia, así que no justifica un job propio.
 *
 * <p>Cada jugador/liga se procesa de forma aislada (constitution.md §1 "manejo explícito de
 * fallos/indisponibilidad"): una excepción al traducir/persistir el rendimiento de un jugador o
 * liga puntual se loguea y no interrumpe la ingesta del resto del catálogo.
 */
@Service
public class RendimientoServiceImpl implements RendimientoService {

    private static final Logger log = LoggerFactory.getLogger(RendimientoServiceImpl.class);

    private static final String METRICAS_SIN_DATOS = "{}";
    private static final String PREFIJO_PARTIDO_EXTERNO_LIGA = "WHOSCORED-LIGA-";

    /**
     * Único conjunto de métricas que la página de liga de WhoScored provee como acumulado real de
     * temporada — a diferencia del detalle por partido (eliminado), esta fuente no admite "sin
     * curación previa" porque no expone más que esto de forma agregada (plan.md §8.1).
     */
    private static final Set<String> METRICAS_PERMITIDAS =
            Set.of(
                    "apps",
                    "goals",
                    "assists",
                    "manOfTheMatch",
                    "yellowCard",
                    "redCard",
                    "minsPlayed");

    private final FixtureRepository fixtureRepository;
    private final RendimientoExternoRepository rendimientoExternoRepository;
    private final RendimientoLigaExternoRepository rendimientoLigaExternoRepository;
    private final RendimientoPartidoRepository rendimientoPartidoRepository;
    private final RendimientoAcumuladoWhoScoredRepository rendimientoAcumuladoWhoScoredRepository;
    private final JugadorRepository jugadorRepository;
    private final ObjectMapper objectMapper;

    public RendimientoServiceImpl(
            FixtureRepository fixtureRepository,
            RendimientoExternoRepository rendimientoExternoRepository,
            RendimientoLigaExternoRepository rendimientoLigaExternoRepository,
            RendimientoPartidoRepository rendimientoPartidoRepository,
            RendimientoAcumuladoWhoScoredRepository rendimientoAcumuladoWhoScoredRepository,
            JugadorRepository jugadorRepository,
            ObjectMapper objectMapper) {
        this.fixtureRepository = fixtureRepository;
        this.rendimientoExternoRepository = rendimientoExternoRepository;
        this.rendimientoLigaExternoRepository = rendimientoLigaExternoRepository;
        this.rendimientoPartidoRepository = rendimientoPartidoRepository;
        this.rendimientoAcumuladoWhoScoredRepository = rendimientoAcumuladoWhoScoredRepository;
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
        List<Jugador> jugadoresActivos = jugadoresActivos();
        if (jugadoresActivos.isEmpty()) {
            return;
        }

        for (Jugador jugador : jugadoresActivos) {
            actualizarFotoYPersistirSiCorresponde(jugador);
        }

        String semanaActual = semanaIsoDe(LocalDate.now());
        Map<String, List<Jugador>> jugadoresPorLiga =
                jugadoresActivos.stream().collect(Collectors.groupingBy(Jugador::getLiga));
        for (Map.Entry<String, List<Jugador>> entry : jugadoresPorLiga.entrySet()) {
            ingestarRendimientoDeLiga(entry.getKey(), entry.getValue(), semanaActual);
        }
    }

    private void ingestarPartidoDeJugador(Jugador jugador, PartidoCrudo partido) {
        try {
            ingestarSiNoExiste(
                    jugador.getId(),
                    partido.partidoExternoId(),
                    partido.fecha(),
                    METRICAS_SIN_DATOS,
                    FuenteResultado.FOOTBALL_DATA);
        } catch (RuntimeException ex) {
            log.error(
                    "Fallo al ingerir el fixture {} del jugador {}: {}",
                    partido.partidoExternoId(),
                    jugador.getId(),
                    ex.getMessage(),
                    ex);
        }
    }

    private void actualizarFotoYPersistirSiCorresponde(Jugador jugador) {
        if (actualizarFotoSiCorresponde(jugador)) {
            jugadorRepository.save(JugadorEntity.desde(jugador));
        }
    }

    private void ingestarRendimientoDeLiga(
            String liga, List<Jugador> jugadoresDeLaLiga, String semanaActual) {
        List<JugadorRendimientoLigaCrudo> crudos;
        try {
            crudos = rendimientoLigaExternoRepository.obtenerAcumuladoLiga(liga);
        } catch (RuntimeException ex) {
            log.error(
                    "Fallo al obtener el acumulado de WhoScored de la liga '{}': {}",
                    liga,
                    ex.getMessage(),
                    ex);
            return;
        }

        for (JugadorRendimientoLigaCrudo crudo : crudos) {
            try {
                procesarCrudoDeJugador(crudo, jugadoresDeLaLiga, semanaActual);
            } catch (RuntimeException ex) {
                log.error(
                        "Fallo al procesar el rendimiento de liga de '{}' ({}): {}",
                        crudo.nombre(),
                        crudo.club(),
                        ex.getMessage(),
                        ex);
            }
        }
    }

    private void procesarCrudoDeJugador(
            JugadorRendimientoLigaCrudo crudo, List<Jugador> candidatos, String semanaActual) {
        Optional<Jugador> jugadorResuelto = resolverJugador(crudo, candidatos);
        if (jugadorResuelto.isEmpty()) {
            log.warn(
                    "No se pudo resolver de forma inequivoca al jugador de WhoScored '{}' ({});"
                            + " se ignora esta fila",
                    crudo.nombre(),
                    crudo.club());
            return;
        }
        Jugador jugador = jugadorResuelto.get();

        Map<String, BigDecimal> actual = filtrarYConvertir(crudo.metricas());
        if (actual.isEmpty()) {
            return;
        }
        String actualJson = serializar(actual);

        Optional<RendimientoAcumuladoWhoScoredEntity> snapshotExistente =
                rendimientoAcumuladoWhoScoredRepository.findById(jugador.getId());
        if (snapshotExistente.isEmpty()) {
            rendimientoAcumuladoWhoScoredRepository.save(
                    RendimientoAcumuladoWhoScoredEntity.desde(
                            RendimientoAcumuladoWhoScored.primeraVez(jugador.getId(), actualJson)));
            return;
        }

        RendimientoAcumuladoWhoScored snapshot = snapshotExistente.get().aModelo();
        Map<String, BigDecimal> anterior = parsearComoBigDecimal(snapshot.getMetricas());
        Map<String, BigDecimal> delta = calcularDelta(anterior, actual);

        snapshot.actualizarSnapshot(actualJson, Instant.now());
        rendimientoAcumuladoWhoScoredRepository.save(
                RendimientoAcumuladoWhoScoredEntity.desde(snapshot));

        if (delta.values().stream().allMatch(valor -> valor.signum() == 0)) {
            return;
        }

        String partidoExternoId = PREFIJO_PARTIDO_EXTERNO_LIGA + semanaActual;
        if (rendimientoPartidoRepository.existsByJugadorIdAndPartidoExternoId(
                jugador.getId(), partidoExternoId)) {
            return;
        }

        ingestarSiNoExiste(
                jugador.getId(),
                partidoExternoId,
                LocalDate.now(),
                serializar(delta),
                FuenteResultado.WHOSCORED_LIGA);
        jugador.registrarActualizacionRendimiento(Instant.now());
        jugadorRepository.save(JugadorEntity.desde(jugador));
    }

    private Optional<Jugador> resolverJugador(
            JugadorRendimientoLigaCrudo crudo, List<Jugador> candidatos) {
        List<Jugador> coincidencias =
                candidatos.stream()
                        .filter(
                                jugador ->
                                        NombreClubMatcher.coincide(
                                                crudo.nombre(),
                                                crudo.club(),
                                                jugador.getNombre(),
                                                jugador.getClub()))
                        .toList();
        return coincidencias.size() == 1 ? Optional.of(coincidencias.get(0)) : Optional.empty();
    }

    private static Map<String, BigDecimal> filtrarYConvertir(Map<String, Object> metricas) {
        Map<String, BigDecimal> resultado = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : metricas.entrySet()) {
            if (METRICAS_PERMITIDAS.contains(entry.getKey())
                    && entry.getValue() instanceof Number numero) {
                resultado.put(entry.getKey(), new BigDecimal(numero.toString()));
            }
        }
        return resultado;
    }

    private static Map<String, BigDecimal> calcularDelta(
            Map<String, BigDecimal> anterior, Map<String, BigDecimal> actual) {
        Map<String, BigDecimal> delta = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : actual.entrySet()) {
            BigDecimal valorAnterior = anterior.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            BigDecimal diferencia = entry.getValue().subtract(valorAnterior);
            delta.put(entry.getKey(), diferencia.signum() < 0 ? BigDecimal.ZERO : diferencia);
        }
        return delta;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EstadisticasTemporada> obtenerEstadisticasTemporada(UUID jugadorId) {
        if (!jugadorRepository.existsById(jugadorId)) {
            throw new RecursoNoEncontradoException("Jugador", jugadorId);
        }
        return rendimientoAcumuladoWhoScoredRepository
                .findById(jugadorId)
                .map(RendimientoAcumuladoWhoScoredEntity::aModelo)
                .map(this::aEstadisticas);
    }

    private EstadisticasTemporada aEstadisticas(RendimientoAcumuladoWhoScored snapshot) {
        Map<String, BigDecimal> metricas = parsearComoBigDecimal(snapshot.getMetricas());
        return new EstadisticasTemporada(
                entero(metricas, "apps"),
                entero(metricas, "minsPlayed"),
                entero(metricas, "goals"),
                entero(metricas, "assists"),
                entero(metricas, "manOfTheMatch"),
                entero(metricas, "yellowCard"),
                entero(metricas, "redCard"),
                snapshot.getFechaActualizacion());
    }

    private static int entero(Map<String, BigDecimal> metricas, String clave) {
        return metricas.getOrDefault(clave, BigDecimal.ZERO).intValue();
    }

    private Map<String, BigDecimal> parsearComoBigDecimal(String json) {
        try {
            Map<String, Object> crudo =
                    objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            Map<String, BigDecimal> resultado = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : crudo.entrySet()) {
                if (entry.getValue() instanceof Number numero) {
                    resultado.put(entry.getKey(), new BigDecimal(numero.toString()));
                }
            }
            return resultado;
        } catch (RuntimeException ex) {
            log.error("Fallo al parsear snapshot de WhoScored '{}': {}", json, ex.getMessage(), ex);
            return Map.of();
        }
    }

    private String serializar(Map<String, BigDecimal> metricas) {
        return objectMapper.writeValueAsString(metricas);
    }

    /**
     * Pide a WhoScored la foto de perfil del jugador y la aplica si trajo una URL nueva o distinta
     * a la ya persistida. Aísla sus propios errores igual que el resto de la ingesta (constitution
     * §1): una falla acá nunca interrumpe la ingesta del resto del catálogo.
     *
     * @return {@code true} si {@code jugador} quedó con una foto nueva pendiente de persistir
     */
    private boolean actualizarFotoSiCorresponde(Jugador jugador) {
        Optional<String> fotoUrl;
        try {
            fotoUrl = rendimientoExternoRepository.obtenerFotoUrl(jugador.getId());
        } catch (RuntimeException ex) {
            log.error(
                    "Fallo al obtener la foto de WhoScored para el jugador {}: {}",
                    jugador.getId(),
                    ex.getMessage(),
                    ex);
            return false;
        }
        if (fotoUrl.isEmpty() || Objects.equals(fotoUrl.get(), jugador.getFotoUrl())) {
            return false;
        }
        jugador.actualizarFoto(fotoUrl.get());
        return true;
    }

    private boolean ingestarSiNoExiste(
            UUID jugadorId,
            String partidoExternoId,
            LocalDate fechaPartido,
            String metricas,
            FuenteResultado fuenteResultado) {
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
                                fuenteResultado)));
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
