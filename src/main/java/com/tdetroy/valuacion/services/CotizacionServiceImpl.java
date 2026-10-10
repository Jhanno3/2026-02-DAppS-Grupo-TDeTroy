package com.tdetroy.valuacion.services;

import com.tdetroy.valuacion.common.Monetario;
import com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException;
import com.tdetroy.valuacion.entity.CotizacionHistoricaEntity;
import com.tdetroy.valuacion.entity.JugadorEntity;
import com.tdetroy.valuacion.entity.PesoMetricaEntity;
import com.tdetroy.valuacion.entity.RendimientoPartidoEntity;
import com.tdetroy.valuacion.model.CotizacionHistorica;
import com.tdetroy.valuacion.model.EstadoJugador;
import com.tdetroy.valuacion.model.Jugador;
import com.tdetroy.valuacion.model.OrigenCotizacion;
import com.tdetroy.valuacion.repositories.CotizacionHistoricaRepository;
import com.tdetroy.valuacion.repositories.JugadorRepository;
import com.tdetroy.valuacion.repositories.PesoMetricaRepository;
import com.tdetroy.valuacion.repositories.RendimientoPartidoRepository;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * <p><b>Normalización del puntaje de rendimiento (plan.md §6.2, asunción técnica de esta
 * implementación):</b> por cada {@code PesoMetrica} activo presente en la semana en curso, el valor
 * se normaliza min-max sobre su propio histórico reciente ({@code
 * app.cotizacion.ventana-historico-semanas}) a un rango <b>centrado en 0</b>, {@code [-1, 1]}
 * (mínimo histórico → -1, máximo histórico → +1), en vez de {@code [0, 1]}: así una métrica en su
 * punto histórico más bajo resta en vez de sumar siempre un poco, que es lo que exige un {@code
 * factorAjuste} que puede ser tanto positivo como negativo. Si el jugador tuvo múltiples {@code
 * RendimientoPartido} en la misma semana, cada métrica se <b>suma</b> entre esos partidos antes de
 * normalizar (ej. {@code minutosJugados} de dos partidos se acumula) — una simplificación uniforme
 * documentada acá; distinguir métricas que deberían promediarse en vez de sumarse (ej. {@code
 * rating}) queda fuera de alcance de esta tarea. El puntaje final es el <b>promedio ponderado</b>
 * (no la suma cruda) de esas normalizaciones — se auto-normaliza frente a cómo estén calibrados los
 * pesos entre sí, así {@code app.cotizacion.sensibilidad} no depende de cuántas métricas activas
 * haya ni de la escala relativa de sus pesos.
 *
 * <p>Si una métrica activa no tiene al menos dos semanas de histórico propio (incluida la semana en
 * curso) para calcular un rango, o ese rango es cero (el jugador repitió exactamente el mismo valor
 * toda la ventana), esa métrica aporta 0 (neutro) a la normalización en vez de participar con un
 * valor arbitrario.
 *
 * <p><b>self (tasks.md T4.5):</b> {@link #recalcular} nunca llama a {@code
 * this.calcularCotizacion(...)} directamente -- una auto-invocacion asi saltea el proxy AOP de
 * Spring y el {@code @Transactional(REQUIRES_NEW)} de {@link #calcularCotizacion} quedaria sin
 * efecto (corriendo sin transaccion propia en vez de una sub-transaccion aislada por jugador,
 * plan.md S5). Se inyecta la propia interfaz via {@code @Lazy} (constructor, no atributo --
 * constitution.md S2) para forzar el paso por el proxy real.
 */
@Service
public class CotizacionServiceImpl implements CotizacionService {

    private static final Logger log = LoggerFactory.getLogger(CotizacionServiceImpl.class);

    private static final String ENTIDAD_JUGADOR = "Jugador";
    private static final String ACCION_RECALCULO = "RECALCULO_COTIZACION";
    private static final BigDecimal CLAMP_MAXIMO = new BigDecimal("0.20");
    private static final BigDecimal CLAMP_MINIMO = CLAMP_MAXIMO.negate();
    private static final BigDecimal DOS = BigDecimal.valueOf(2);

    private final RendimientoPartidoRepository rendimientoPartidoRepository;
    private final CotizacionHistoricaRepository cotizacionHistoricaRepository;
    private final PesoMetricaRepository pesoMetricaRepository;
    private final JugadorRepository jugadorRepository;
    private final AuditoriaService auditoriaService;
    private final ObjectMapper objectMapper;
    private final CotizacionService self;
    private final BigDecimal valorInicial;
    private final BigDecimal sensibilidad;
    private final BigDecimal decayInactividad;
    private final long ventanaHistoricoSemanas;

    public CotizacionServiceImpl(
            RendimientoPartidoRepository rendimientoPartidoRepository,
            CotizacionHistoricaRepository cotizacionHistoricaRepository,
            PesoMetricaRepository pesoMetricaRepository,
            JugadorRepository jugadorRepository,
            AuditoriaService auditoriaService,
            ObjectMapper objectMapper,
            @Lazy CotizacionService self,
            @Value("${app.cotizacion.valor-inicial}") BigDecimal valorInicial,
            @Value("${app.cotizacion.sensibilidad}") BigDecimal sensibilidad,
            @Value("${app.cotizacion.decay-inactividad}") BigDecimal decayInactividad,
            @Value("${app.cotizacion.ventana-historico-semanas}") long ventanaHistoricoSemanas) {
        this.rendimientoPartidoRepository = rendimientoPartidoRepository;
        this.cotizacionHistoricaRepository = cotizacionHistoricaRepository;
        this.pesoMetricaRepository = pesoMetricaRepository;
        this.jugadorRepository = jugadorRepository;
        this.auditoriaService = auditoriaService;
        this.objectMapper = objectMapper;
        this.self = self;
        this.valorInicial = valorInicial;
        this.sensibilidad = sensibilidad;
        this.decayInactividad = decayInactividad;
        this.ventanaHistoricoSemanas = ventanaHistoricoSemanas;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CotizacionHistorica calcularCotizacion(
            UUID jugadorId, OrigenCotizacion origen, UUID actorId) {
        Objects.requireNonNull(jugadorId, "jugadorId no puede ser null");
        Objects.requireNonNull(origen, "origen no puede ser null");

        JugadorEntity jugadorEntity =
                jugadorRepository
                        .findById(jugadorId)
                        .orElseThrow(
                                () -> new RecursoNoEncontradoException(ENTIDAD_JUGADOR, jugadorId));

        LocalDate hoy = LocalDate.now();
        String semanaActual = semanaIsoDe(hoy);
        String semanaDesde = semanaIsoDe(hoy.minusWeeks(ventanaHistoricoSemanas));

        List<RendimientoPartidoEntity> historico =
                rendimientoPartidoRepository.findByJugadorIdAndSemanaCalculoGreaterThanEqual(
                        jugadorId, semanaDesde);
        Map<String, Map<String, BigDecimal>> metricasPorSemana = agruparPorSemana(historico);
        Map<String, BigDecimal> metricasSemanaActual =
                metricasPorSemana.getOrDefault(semanaActual, Map.of());

        BigDecimal factorAjuste =
                metricasSemanaActual.isEmpty()
                        ? decayInactividad.negate()
                        : clamp(
                                calcularPuntajeRendimiento(metricasPorSemana, metricasSemanaActual)
                                        .multiply(sensibilidad));

        Optional<CotizacionHistoricaEntity> cotizacionVigente =
                cotizacionHistoricaRepository.findTopByJugadorIdOrderByFechaCalculoDesc(jugadorId);
        BigDecimal cotizacionAnterior =
                cotizacionVigente.map(CotizacionHistoricaEntity::getValor).orElse(valorInicial);
        BigDecimal cotizacionNueva =
                Monetario.escalar(cotizacionAnterior.multiply(BigDecimal.ONE.add(factorAjuste)));

        CotizacionHistorica cotizacion =
                CotizacionHistorica.calcular(jugadorId, semanaActual, cotizacionNueva, origen);
        cotizacionHistoricaRepository.save(CotizacionHistoricaEntity.desde(cotizacion));

        Jugador jugador = jugadorEntity.aModelo();
        jugador.actualizarCotizacionVigente(cotizacion.getId());
        jugadorRepository.save(JugadorEntity.desde(jugador));

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
        Objects.requireNonNull(origen, "origen no puede ser null");

        for (JugadorEntity jugadorEntity : jugadorRepository.findByEstado(EstadoJugador.ACTIVO)) {
            UUID jugadorId = jugadorEntity.getId();
            try {
                // Vía `self` (proxy de Spring): asegura que @Transactional(REQUIRES_NEW) de
                // calcularCotizacion efectivamente abra una sub-transacción propia por jugador
                // (plan.md §5) -- una auto-invocación `this.calcularCotizacion(...)` saltearía el
                // proxy AOP y correría sin transacción propia.
                self.calcularCotizacion(jugadorId, origen, actorId);
            } catch (RuntimeException ex) {
                log.error(
                        "Fallo al recalcular la cotización del jugador {}: {}",
                        jugadorId,
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

    /**
     * Puntaje en [-1, 1]: promedio ponderado de la normalización min-max de cada métrica activa.
     */
    private BigDecimal calcularPuntajeRendimiento(
            Map<String, Map<String, BigDecimal>> metricasPorSemana,
            Map<String, BigDecimal> metricasSemanaActual) {
        List<PesoMetricaEntity> pesosActivos = pesoMetricaRepository.findByActivoTrue();

        BigDecimal sumaPonderada = BigDecimal.ZERO;
        BigDecimal sumaPesos = BigDecimal.ZERO;
        for (PesoMetricaEntity pesoMetrica : pesosActivos) {
            BigDecimal valorActual = metricasSemanaActual.get(pesoMetrica.getClave());
            if (valorActual == null) {
                continue;
            }
            List<BigDecimal> historicoDeLaClave =
                    metricasPorSemana.values().stream()
                            .map(
                                    metricasDeEsaSemana ->
                                            metricasDeEsaSemana.get(pesoMetrica.getClave()))
                            .filter(Objects::nonNull)
                            .toList();
            BigDecimal normalizado = normalizarMinMax(valorActual, historicoDeLaClave);
            sumaPonderada = sumaPonderada.add(pesoMetrica.getPeso().multiply(normalizado));
            sumaPesos = sumaPesos.add(pesoMetrica.getPeso());
        }

        return sumaPesos.signum() == 0
                ? BigDecimal.ZERO
                : sumaPonderada.divide(sumaPesos, MathContext.DECIMAL64);
    }

    private static BigDecimal normalizarMinMax(BigDecimal valorActual, List<BigDecimal> historico) {
        if (historico.size() < 2) {
            return BigDecimal.ZERO;
        }
        BigDecimal min = historico.stream().min(Comparator.naturalOrder()).orElseThrow();
        BigDecimal max = historico.stream().max(Comparator.naturalOrder()).orElseThrow();
        BigDecimal rango = max.subtract(min);
        if (rango.signum() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal posicion = valorActual.subtract(min).divide(rango, MathContext.DECIMAL64);
        return posicion.multiply(DOS).subtract(BigDecimal.ONE);
    }

    /** {@code semanaCalculo -> (clave de métrica -> suma de esa métrica en esa semana)}. */
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
        } catch (RuntimeException ex) {
            log.error("Fallo al parsear metricas '{}': {}", metricasJson, ex.getMessage(), ex);
            return Map.of();
        }
    }

    private static BigDecimal clamp(BigDecimal valor) {
        if (valor.compareTo(CLAMP_MAXIMO) > 0) {
            return CLAMP_MAXIMO;
        }
        if (valor.compareTo(CLAMP_MINIMO) < 0) {
            return CLAMP_MINIMO;
        }
        return valor;
    }

    private static String semanaIsoDe(LocalDate fecha) {
        int semana = fecha.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int anio = fecha.get(IsoFields.WEEK_BASED_YEAR);
        return "%04d-W%02d".formatted(anio, semana);
    }
}
