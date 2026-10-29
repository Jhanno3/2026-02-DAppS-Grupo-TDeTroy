package com.tdetroy.valuacion.services;

import com.tdetroy.valuacion.common.Monetario;
import com.tdetroy.valuacion.common.SemanaIso;
import com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException;
import com.tdetroy.valuacion.entity.ConfiguracionCotizacionEntity;
import com.tdetroy.valuacion.entity.CotizacionHistoricaEntity;
import com.tdetroy.valuacion.entity.JugadorEntity;
import com.tdetroy.valuacion.entity.PesoMetricaEntity;
import com.tdetroy.valuacion.entity.RendimientoPartidoEntity;
import com.tdetroy.valuacion.model.ConfiguracionCotizacion;
import com.tdetroy.valuacion.model.CotizacionHistorica;
import com.tdetroy.valuacion.model.EstadoJugador;
import com.tdetroy.valuacion.model.EstrategiaCotizacion;
import com.tdetroy.valuacion.model.Jugador;
import com.tdetroy.valuacion.model.OrigenCotizacion;
import com.tdetroy.valuacion.repositories.ConfiguracionCotizacionRepository;
import com.tdetroy.valuacion.repositories.CotizacionHistoricaRepository;
import com.tdetroy.valuacion.repositories.JugadorRepository;
import com.tdetroy.valuacion.repositories.PesoMetricaRepository;
import com.tdetroy.valuacion.repositories.RendimientoPartidoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * {@inheritDoc}
 *
 * <p>Motor de cálculo de cotización que soporta múltiples estrategias (Strategy pattern) vía {@link
 * EstrategiaCotizacionCalculator}:
 *
 * <ul>
 *   <li>{@link EstrategiaCotizacion#RENDIMIENTO_TEMPORADA}: min-max relativo al histórico del
 *       jugador.
 *   <li>{@link EstrategiaCotizacion#RENDIMIENTO_ACTUAL}: valuación absoluta del rendimiento
 *       reciente.
 * </ul>
 */
@Service
public class CotizacionServiceImpl implements CotizacionService {

    private static final Logger log = LoggerFactory.getLogger(CotizacionServiceImpl.class);

    private static final String ENTIDAD_JUGADOR = "Jugador";
    private static final String ENTIDAD_CONFIGURACION = "ConfiguracionCotizacion";
    private static final String ACCION_RECALCULO = "RECALCULO_COTIZACION";
    private static final String ACCION_CAMBIO_ESTRATEGIA = "CAMBIO_ESTRATEGIA_COTIZACION";
    private static final BigDecimal CLAMP_MAXIMO = new BigDecimal("0.20");
    private static final BigDecimal CLAMP_MINIMO = CLAMP_MAXIMO.negate();

    private final RendimientoPartidoRepository rendimientoPartidoRepository;
    private final CotizacionHistoricaRepository cotizacionHistoricaRepository;
    private final PesoMetricaRepository pesoMetricaRepository;
    private final JugadorRepository jugadorRepository;
    private final AuditoriaService auditoriaService;
    private final ConfiguracionCotizacionRepository configuracionCotizacionRepository;
    private final ObjectMapper objectMapper;
    private final Map<EstrategiaCotizacion, EstrategiaCotizacionCalculator> calculadores;
    private final CotizacionService self;
    private final BigDecimal valorInicial;
    private final BigDecimal sensibilidad;
    private final BigDecimal decayInactividad;
    private final long ventanaHistoricoSemanas;
    private final EstrategiaCotizacion estrategiaDefaultDeConfiguracion;

    @Autowired
    public CotizacionServiceImpl(
            RendimientoPartidoRepository rendimientoPartidoRepository,
            CotizacionHistoricaRepository cotizacionHistoricaRepository,
            PesoMetricaRepository pesoMetricaRepository,
            JugadorRepository jugadorRepository,
            AuditoriaService auditoriaService,
            ConfiguracionCotizacionRepository configuracionCotizacionRepository,
            ObjectMapper objectMapper,
            List<EstrategiaCotizacionCalculator> calculadoresList,
            @Lazy CotizacionService self,
            @Value("${app.cotizacion.valor-inicial}") BigDecimal valorInicial,
            @Value("${app.cotizacion.sensibilidad}") BigDecimal sensibilidad,
            @Value("${app.cotizacion.decay-inactividad}") BigDecimal decayInactividad,
            @Value("${app.cotizacion.ventana-historico-semanas}") long ventanaHistoricoSemanas,
            @Value("${app.cotizacion.estrategia-default:RENDIMIENTO_ACTUAL}")
                    String estrategiaDefaultStr) {
        this.rendimientoPartidoRepository = rendimientoPartidoRepository;
        this.cotizacionHistoricaRepository = cotizacionHistoricaRepository;
        this.pesoMetricaRepository = pesoMetricaRepository;
        this.jugadorRepository = jugadorRepository;
        this.auditoriaService = auditoriaService;
        this.configuracionCotizacionRepository = configuracionCotizacionRepository;
        this.objectMapper = objectMapper;
        this.self = self;
        this.valorInicial = valorInicial;
        this.sensibilidad = sensibilidad;
        this.decayInactividad = decayInactividad;
        this.ventanaHistoricoSemanas = ventanaHistoricoSemanas;

        EstrategiaCotizacion parsedDefault = EstrategiaCotizacion.RENDIMIENTO_ACTUAL;
        if (estrategiaDefaultStr != null && !estrategiaDefaultStr.isBlank()) {
            try {
                parsedDefault = EstrategiaCotizacion.valueOf(estrategiaDefaultStr.trim());
            } catch (IllegalArgumentException ex) {
                log.warn(
                        "Estrategia default '{}' no reconocida, usando {}",
                        estrategiaDefaultStr,
                        parsedDefault);
            }
        }
        this.estrategiaDefaultDeConfiguracion = parsedDefault;

        this.calculadores = new EnumMap<>(EstrategiaCotizacion.class);
        if (calculadoresList != null) {
            for (EstrategiaCotizacionCalculator calc : calculadoresList) {
                this.calculadores.put(calc.getEstrategia(), calc);
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CotizacionHistorica calcularCotizacion(
            UUID jugadorId, OrigenCotizacion origen, UUID actorId) {
        return calcular(jugadorId, origen, null, actorId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CotizacionHistorica calcularCotizacion(
            UUID jugadorId,
            OrigenCotizacion origen,
            EstrategiaCotizacion estrategia,
            UUID actorId) {
        return calcular(jugadorId, origen, estrategia, actorId);
    }

    // Sin @Transactional: corre dentro de la transacción que abre cada overload público vía el
    // proxy. Una overload llamando a la otra con "this." saltearía el proxy y su REQUIRES_NEW.
    private CotizacionHistorica calcular(
            UUID jugadorId,
            OrigenCotizacion origen,
            EstrategiaCotizacion estrategia,
            UUID actorId) {
        Objects.requireNonNull(jugadorId, "jugadorId no puede ser null");
        Objects.requireNonNull(origen, "origen no puede ser null");
        EstrategiaCotizacion estrategiaPorDefecto = obtenerEstrategiaPorDefecto();
        EstrategiaCotizacion estrategiaEfectiva =
                estrategia != null ? estrategia : estrategiaPorDefecto;

        JugadorEntity jugadorEntity =
                jugadorRepository
                        .findById(jugadorId)
                        .orElseThrow(
                                () -> new RecursoNoEncontradoException(ENTIDAD_JUGADOR, jugadorId));

        LocalDate referencia = SemanaIso.fechaDeReferencia(LocalDate.now());
        String semanaActual = SemanaIso.de(referencia);
        String semanaDesde = SemanaIso.de(referencia.minusWeeks(ventanaHistoricoSemanas));

        List<RendimientoPartidoEntity> historico =
                rendimientoPartidoRepository.findByJugadorIdAndSemanaCalculoGreaterThanEqual(
                        jugadorId, semanaDesde);
        Map<String, Map<String, BigDecimal>> metricasPorSemana = agruparPorSemana(historico);
        Map<String, BigDecimal> metricasSemanaActual =
                metricasPorSemana.getOrDefault(semanaActual, Map.of());

        BigDecimal factorAjuste;
        if (metricasSemanaActual.isEmpty()) {
            factorAjuste = decayInactividad.negate();
        } else {
            EstrategiaCotizacionCalculator calculator =
                    calculadores.getOrDefault(
                            estrategiaEfectiva, calculadores.get(estrategiaPorDefecto));
            if (calculator == null) {
                calculator = new RendimientoActualCalculator();
            }
            List<PesoMetricaEntity> pesosActivos = pesoMetricaRepository.findByActivoTrue();
            BigDecimal puntaje =
                    calculator.calcularPuntaje(
                            metricasPorSemana, metricasSemanaActual, pesosActivos);
            factorAjuste = clamp(puntaje.multiply(sensibilidad));
        }

        Optional<CotizacionHistoricaEntity> cotizacionVigente =
                cotizacionHistoricaRepository
                        .findTopByJugadorIdAndEstrategiaOrderByFechaCalculoDesc(
                                jugadorId, estrategiaEfectiva)
                        .or(
                                () ->
                                        cotizacionHistoricaRepository
                                                .findTopByJugadorIdOrderByFechaCalculoDesc(
                                                        jugadorId));
        BigDecimal cotizacionAnterior =
                cotizacionVigente.map(CotizacionHistoricaEntity::getValor).orElse(valorInicial);
        BigDecimal cotizacionNueva =
                Monetario.escalar(cotizacionAnterior.multiply(BigDecimal.ONE.add(factorAjuste)));

        CotizacionHistorica cotizacion =
                CotizacionHistorica.calcular(
                        jugadorId, semanaActual, cotizacionNueva, origen, estrategiaEfectiva);
        cotizacionHistoricaRepository.save(CotizacionHistoricaEntity.desde(cotizacion));

        if (estrategiaEfectiva == estrategiaPorDefecto
                || jugadorEntity.getCotizacionVigenteId() == null) {
            Jugador jugador = jugadorEntity.aModelo();
            jugador.actualizarCotizacionVigente(cotizacion.getId());
            jugadorRepository.save(JugadorEntity.desde(jugador));
        }

        auditoriaService.registrar(
                actorId,
                ACCION_RECALCULO,
                ENTIDAD_JUGADOR,
                jugadorId,
                cotizacionVigente.map(CotizacionHistoricaEntity::getValor).orElse(null),
                cotizacion.getValor());

        return cotizacion;
    }

    @Override
    public void recalcular(OrigenCotizacion origen, UUID actorId) {
        recalcular(origen, null, actorId);
    }

    @Override
    public void recalcular(OrigenCotizacion origen, EstrategiaCotizacion estrategia, UUID actorId) {
        Objects.requireNonNull(origen, "origen no puede ser null");
        EstrategiaCotizacion estrategiaEfectiva =
                estrategia != null ? estrategia : obtenerEstrategiaPorDefecto();

        for (JugadorEntity jugadorEntity : jugadorRepository.findByEstado(EstadoJugador.ACTIVO)) {
            UUID jugadorId = jugadorEntity.getId();
            try {
                self.calcularCotizacion(jugadorId, origen, estrategiaEfectiva, actorId);
            } catch (RuntimeException ex) {
                log.error(
                        "Fallo al recalcular la cotización del jugador {} con estrategia {}: {}",
                        jugadorId,
                        estrategiaEfectiva,
                        ex.getMessage(),
                        ex);
            }
        }
    }

    @Override
    public List<CotizacionHistorica> obtenerHistorial(UUID jugadorId) {
        Objects.requireNonNull(jugadorId, "jugadorId no puede ser null");
        if (!jugadorRepository.existsById(jugadorId)) {
            throw new RecursoNoEncontradoException(ENTIDAD_JUGADOR, jugadorId);
        }
        return cotizacionHistoricaRepository
                .findByJugadorIdOrderByFechaCalculoAsc(jugadorId)
                .stream()
                .map(CotizacionHistoricaEntity::aModelo)
                .toList();
    }

    @Override
    public List<CotizacionHistorica> obtenerHistorial(
            UUID jugadorId, EstrategiaCotizacion estrategia) {
        Objects.requireNonNull(jugadorId, "jugadorId no puede ser null");
        if (!jugadorRepository.existsById(jugadorId)) {
            throw new RecursoNoEncontradoException(ENTIDAD_JUGADOR, jugadorId);
        }
        if (estrategia == null) {
            return obtenerHistorial(jugadorId);
        }
        return cotizacionHistoricaRepository
                .findByJugadorIdAndEstrategiaOrderByFechaCalculoAsc(jugadorId, estrategia)
                .stream()
                .map(CotizacionHistoricaEntity::aModelo)
                .toList();
    }

    @Override
    public Optional<CotizacionHistorica> obtenerVigente(
            UUID jugadorId, EstrategiaCotizacion estrategia) {
        Objects.requireNonNull(jugadorId, "jugadorId no puede ser null");
        EstrategiaCotizacion estrategiaEfectiva =
                estrategia != null ? estrategia : obtenerEstrategiaPorDefecto();
        return cotizacionHistoricaRepository
                .findTopByJugadorIdAndEstrategiaOrderByFechaCalculoDesc(
                        jugadorId, estrategiaEfectiva)
                .map(CotizacionHistoricaEntity::aModelo);
    }

    @Override
    public CotizacionHistorica obtenerDeSemana(
            UUID jugadorId, String semana, EstrategiaCotizacion estrategia) {
        Objects.requireNonNull(jugadorId, "jugadorId no puede ser null");
        Objects.requireNonNull(semana, "semana no puede ser null");
        if (!jugadorRepository.existsById(jugadorId)) {
            throw new RecursoNoEncontradoException(ENTIDAD_JUGADOR, jugadorId);
        }
        Optional<CotizacionHistoricaEntity> cotizacion =
                estrategia == null
                        ? cotizacionHistoricaRepository
                                .findTopByJugadorIdAndSemanaOrderByFechaCalculoDesc(
                                        jugadorId, semana)
                        : cotizacionHistoricaRepository
                                .findTopByJugadorIdAndSemanaAndEstrategiaOrderByFechaCalculoDesc(
                                        jugadorId, semana, estrategia);
        return cotizacion
                .map(CotizacionHistoricaEntity::aModelo)
                .orElseThrow(
                        () ->
                                new RecursoNoEncontradoException(
                                        "El jugador %s no tiene cotización en la semana %s%s"
                                                .formatted(
                                                        jugadorId,
                                                        semana,
                                                        estrategia == null
                                                                ? ""
                                                                : " con estrategia "
                                                                        + estrategia)));
    }

    @Override
    public EstrategiaCotizacion obtenerEstrategiaPorDefecto() {
        return configuracionCotizacionRepository
                .findById(ConfiguracionCotizacion.ID)
                .map(ConfiguracionCotizacionEntity::getEstrategiaDefault)
                .orElse(estrategiaDefaultDeConfiguracion);
    }

    @Override
    @Transactional
    public void cambiarEstrategiaPorDefecto(EstrategiaCotizacion estrategia, UUID actorId) {
        Objects.requireNonNull(estrategia, "estrategia no puede ser null");
        EstrategiaCotizacion anterior = obtenerEstrategiaPorDefecto();
        ConfiguracionCotizacion configuracion =
                configuracionCotizacionRepository
                        .findById(ConfiguracionCotizacion.ID)
                        .map(ConfiguracionCotizacionEntity::aModelo)
                        .orElseGet(() -> ConfiguracionCotizacion.configurar(estrategia));
        configuracion.cambiarEstrategiaDefault(estrategia);
        configuracionCotizacionRepository.save(ConfiguracionCotizacionEntity.desde(configuracion));

        auditoriaService.registrar(
                actorId,
                ACCION_CAMBIO_ESTRATEGIA,
                ENTIDAD_CONFIGURACION,
                ConfiguracionCotizacion.ID,
                Map.of("estrategiaDefault", anterior),
                Map.of("estrategiaDefault", estrategia));
    }

    private static BigDecimal clamp(BigDecimal factorAjuste) {
        if (factorAjuste.compareTo(CLAMP_MAXIMO) > 0) {
            return CLAMP_MAXIMO;
        }
        if (factorAjuste.compareTo(CLAMP_MINIMO) < 0) {
            return CLAMP_MINIMO;
        }
        return factorAjuste;
    }

    private Map<String, Map<String, BigDecimal>> agruparPorSemana(
            List<RendimientoPartidoEntity> historico) {
        Map<String, Map<String, BigDecimal>> metricasPorSemana = new HashMap<>();
        for (RendimientoPartidoEntity partido : historico) {
            Map<String, BigDecimal> metricasDeLaSemana =
                    metricasPorSemana.computeIfAbsent(
                            partido.getSemanaCalculo(), semana -> new HashMap<>());
            for (Map.Entry<String, BigDecimal> entry :
                    parsearMetricas(partido.getMetricas()).entrySet()) {
                metricasDeLaSemana.merge(entry.getKey(), entry.getValue(), BigDecimal::add);
            }
        }
        return metricasPorSemana;
    }

    private Map<String, BigDecimal> parsearMetricas(String metricasJson) {
        try {
            Map<String, Object> crudo =
                    objectMapper.readValue(
                            metricasJson, new TypeReference<Map<String, Object>>() {});
            Map<String, BigDecimal> numericas = new HashMap<>();
            for (Map.Entry<String, Object> entry : crudo.entrySet()) {
                if (entry.getValue() instanceof Number numero) {
                    numericas.put(entry.getKey(), new BigDecimal(numero.toString()));
                }
            }
            return numericas;
        } catch (Exception ex) {
            log.warn("No se pudieron parsear las métricas JSON: {}", metricasJson, ex);
            return Map.of();
        }
    }
}
