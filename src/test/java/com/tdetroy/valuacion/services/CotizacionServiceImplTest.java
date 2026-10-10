package com.tdetroy.valuacion.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException;
import com.tdetroy.valuacion.entity.CotizacionHistoricaEntity;
import com.tdetroy.valuacion.entity.JugadorEntity;
import com.tdetroy.valuacion.entity.PesoMetricaEntity;
import com.tdetroy.valuacion.entity.RendimientoPartidoEntity;
import com.tdetroy.valuacion.model.CotizacionHistorica;
import com.tdetroy.valuacion.model.EstadoJugador;
import com.tdetroy.valuacion.model.FuenteResultado;
import com.tdetroy.valuacion.model.Jugador;
import com.tdetroy.valuacion.model.OrigenCotizacion;
import com.tdetroy.valuacion.repositories.CotizacionHistoricaRepository;
import com.tdetroy.valuacion.repositories.JugadorRepository;
import com.tdetroy.valuacion.repositories.PesoMetricaRepository;
import com.tdetroy.valuacion.repositories.RendimientoPartidoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

/**
 * Cubre {@link CotizacionServiceImpl} (tasks.md T4.3/T4.4/T4.5, plan.md §6.1-§6.4): primer cálculo
 * sin cotización previa (usa {@code valorInicial}), jugador activo sin rendimiento en la semana en
 * curso (decaimiento leve, §6.3), cálculo ponderado normal con métricas/pesos normalizados min-max
 * sobre el histórico reciente del jugador, el clamp de ±20% sobre {@code factorAjuste} (§6.1 paso
 * 3) en ambas direcciones, la auditoría de cada recálculo, y la orquestación de {@code recalcular}
 * sobre todos los jugadores {@code ACTIVO} (§6.4) aislando la falla de uno puntual.
 *
 * <p>{@code semanaActualEsperada()} replica {@code CotizacionServiceImpl#semanaIsoDe} para no
 * hardcodear la semana ISO en curso — a diferencia de {@code RendimientoPartido.semanaCalculo} (que
 * sale de la fecha del partido, un dato fijo del test), acá la semana depende de {@code
 * LocalDate.now()} en el momento en que corre el test.
 */
@ExtendWith(MockitoExtension.class)
class CotizacionServiceImplTest {

    private static final BigDecimal VALOR_INICIAL = new BigDecimal("100.00");
    private static final BigDecimal SENSIBILIDAD_DEFAULT = new BigDecimal("0.20");
    private static final BigDecimal DECAY_INACTIVIDAD = new BigDecimal("0.01");
    private static final long VENTANA_HISTORICO_SEMANAS = 8L;

    @Mock private RendimientoPartidoRepository rendimientoPartidoRepository;
    @Mock private CotizacionHistoricaRepository cotizacionHistoricaRepository;
    @Mock private PesoMetricaRepository pesoMetricaRepository;
    @Mock private JugadorRepository jugadorRepository;
    @Mock private AuditoriaService auditoriaService;
    @Mock private CotizacionService self;

    private UUID jugadorId;
    private UUID actorId;
    private CotizacionServiceImpl service;

    @BeforeEach
    void setUp() {
        jugadorId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        service = construirServicio(SENSIBILIDAD_DEFAULT);
    }

    /** Sólo la necesitan los tests que ejercitan {@code calcularCotizacion} directamente. */
    private void stubJugadorExistente() {
        when(jugadorRepository.findById(jugadorId))
                .thenReturn(Optional.of(JugadorEntity.desde(jugadorActivoCon(jugadorId))));
    }

    private CotizacionServiceImpl construirServicio(BigDecimal sensibilidad) {
        return new CotizacionServiceImpl(
                rendimientoPartidoRepository,
                cotizacionHistoricaRepository,
                pesoMetricaRepository,
                jugadorRepository,
                auditoriaService,
                new ObjectMapper(),
                self,
                VALOR_INICIAL,
                sensibilidad,
                DECAY_INACTIVIDAD,
                VENTANA_HISTORICO_SEMANAS);
    }

    private static Jugador jugadorActivoCon(UUID id) {
        return Jugador.reconstruir(
                id,
                "Jugador de prueba",
                "Club",
                "Delantero",
                LocalDate.of(2000, 1, 1),
                "Argentina",
                EstadoJugador.ACTIVO,
                0,
                null,
                null);
    }

    // ---- calcularCotizacion ----

    @Test
    void jugadorInexistente_lanzaRecursoNoEncontrado() {
        when(jugadorRepository.findById(jugadorId)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.calcularCotizacion(
                                        jugadorId, OrigenCotizacion.AUTOMATICO, actorId))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void sinRendimientoEnLaSemanaActual_aplicaDecaimientoSobreValorInicial() {
        stubJugadorExistente();
        when(rendimientoPartidoRepository.findByJugadorIdAndSemanaCalculoGreaterThanEqual(
                        any(), any()))
                .thenReturn(List.of());
        when(cotizacionHistoricaRepository.findTopByJugadorIdOrderByFechaCalculoDesc(jugadorId))
                .thenReturn(Optional.empty());

        CotizacionHistorica cotizacion =
                service.calcularCotizacion(jugadorId, OrigenCotizacion.AUTOMATICO, actorId);

        // 100.00 * (1 - 0.01) = 99.00
        assertThat(cotizacion.getValor()).isEqualByComparingTo(new BigDecimal("99.00"));
        assertThat(cotizacion.getSemana()).isEqualTo(semanaActualEsperada());
        assertThat(cotizacion.getOrigen()).isEqualTo(OrigenCotizacion.AUTOMATICO);
        verificarPersistenciaYPunteroDelJugador(cotizacion);
    }

    @Test
    void conRendimientoEstaSemanaPeroSinPesosActivos_puntajeNeutroDejaCotizacionAnteriorIntacta() {
        stubJugadorExistente();
        when(rendimientoPartidoRepository.findByJugadorIdAndSemanaCalculoGreaterThanEqual(
                        any(), any()))
                .thenReturn(List.of(rendimientoDe(semanaActualEsperada(), "{\"goles\":1}")));
        when(pesoMetricaRepository.findByActivoTrue()).thenReturn(List.of());
        when(cotizacionHistoricaRepository.findTopByJugadorIdOrderByFechaCalculoDesc(jugadorId))
                .thenReturn(Optional.of(cotizacionAnteriorDe(new BigDecimal("150.00"))));

        CotizacionHistorica cotizacion =
                service.calcularCotizacion(jugadorId, OrigenCotizacion.AUTOMATICO, actorId);

        assertThat(cotizacion.getValor()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    void primerCalculoSinCotizacionPrevia_usaValorInicialComoBase() {
        stubJugadorExistente();
        when(rendimientoPartidoRepository.findByJugadorIdAndSemanaCalculoGreaterThanEqual(
                        any(), any()))
                .thenReturn(List.of());
        when(cotizacionHistoricaRepository.findTopByJugadorIdOrderByFechaCalculoDesc(jugadorId))
                .thenReturn(Optional.empty());

        CotizacionHistorica cotizacion =
                service.calcularCotizacion(jugadorId, OrigenCotizacion.MANUAL, actorId);

        // sin historial (decae desde VALOR_INICIAL, no desde cero ni desde null)
        assertThat(cotizacion.getValor()).isEqualByComparingTo(new BigDecimal("99.00"));
    }

    @Test
    void metricasEnSuMaximoHistorico_sube20PorCientoConSensibilidadDefault() {
        stubJugadorExistente();
        when(rendimientoPartidoRepository.findByJugadorIdAndSemanaCalculoGreaterThanEqual(
                        any(), any()))
                .thenReturn(historicoConMaximoEnLaSemanaActual());
        when(pesoMetricaRepository.findByActivoTrue())
                .thenReturn(List.of(pesoDe("goles", "2.0000"), pesoDe("asistencias", "1.0000")));
        when(cotizacionHistoricaRepository.findTopByJugadorIdOrderByFechaCalculoDesc(jugadorId))
                .thenReturn(Optional.of(cotizacionAnteriorDe(new BigDecimal("200.00"))));

        CotizacionHistorica cotizacion =
                service.calcularCotizacion(jugadorId, OrigenCotizacion.AUTOMATICO, actorId);

        // puntaje = 1.0 (ambas métricas en su máximo histórico) * sensibilidad 0.20 = +0.20,
        // justo en el borde del clamp sin necesitar que lo recorte: 200.00 * 1.20 = 240.00
        assertThat(cotizacion.getValor()).isEqualByComparingTo(new BigDecimal("240.00"));
    }

    @Test
    void factorAjusteSobreElLimiteSuperior_seAcotaAMas20PorCiento() {
        stubJugadorExistente();
        CotizacionServiceImpl servicioSensible = construirServicio(BigDecimal.ONE);
        when(rendimientoPartidoRepository.findByJugadorIdAndSemanaCalculoGreaterThanEqual(
                        any(), any()))
                .thenReturn(historicoConMaximoEnLaSemanaActual());
        when(pesoMetricaRepository.findByActivoTrue())
                .thenReturn(List.of(pesoDe("goles", "2.0000"), pesoDe("asistencias", "1.0000")));
        when(cotizacionHistoricaRepository.findTopByJugadorIdOrderByFechaCalculoDesc(jugadorId))
                .thenReturn(Optional.of(cotizacionAnteriorDe(new BigDecimal("200.00"))));

        CotizacionHistorica cotizacion =
                servicioSensible.calcularCotizacion(
                        jugadorId, OrigenCotizacion.AUTOMATICO, actorId);

        // puntaje = 1.0 * sensibilidad 1.00 = 1.00 sin clamp -> hubiese sido 200.00 * 2.00 =
        // 400.00; con el clamp a +0.20, queda en 200.00 * 1.20 = 240.00
        assertThat(cotizacion.getValor()).isEqualByComparingTo(new BigDecimal("240.00"));
    }

    @Test
    void factorAjusteBajoElLimiteInferior_seAcotaAMenos20PorCiento() {
        stubJugadorExistente();
        CotizacionServiceImpl servicioSensible = construirServicio(BigDecimal.ONE);
        when(rendimientoPartidoRepository.findByJugadorIdAndSemanaCalculoGreaterThanEqual(
                        any(), any()))
                .thenReturn(historicoConMinimoEnLaSemanaActual());
        when(pesoMetricaRepository.findByActivoTrue())
                .thenReturn(List.of(pesoDe("goles", "2.0000"), pesoDe("asistencias", "1.0000")));
        when(cotizacionHistoricaRepository.findTopByJugadorIdOrderByFechaCalculoDesc(jugadorId))
                .thenReturn(Optional.of(cotizacionAnteriorDe(new BigDecimal("200.00"))));

        CotizacionHistorica cotizacion =
                servicioSensible.calcularCotizacion(
                        jugadorId, OrigenCotizacion.AUTOMATICO, actorId);

        // puntaje = -1.0 (ambas métricas en su mínimo histórico) * sensibilidad 1.00 = -1.00 sin
        // clamp -> hubiese sido 200.00 * 0.00 = 0.00; con el clamp a -0.20, queda en
        // 200.00 * 0.80 = 160.00
        assertThat(cotizacion.getValor()).isEqualByComparingTo(new BigDecimal("160.00"));
    }

    // ---- auditoría ----

    @Test
    void primerCalculo_auditaConValoresAntesNuloYDespuesLaCotizacionNueva() {
        stubJugadorExistente();
        when(rendimientoPartidoRepository.findByJugadorIdAndSemanaCalculoGreaterThanEqual(
                        any(), any()))
                .thenReturn(List.of());
        when(cotizacionHistoricaRepository.findTopByJugadorIdOrderByFechaCalculoDesc(jugadorId))
                .thenReturn(Optional.empty());

        CotizacionHistorica cotizacion =
                service.calcularCotizacion(jugadorId, OrigenCotizacion.MANUAL, actorId);

        verify(auditoriaService)
                .registrar(
                        eq(actorId),
                        eq("RECALCULO_COTIZACION"),
                        eq("Jugador"),
                        eq(jugadorId),
                        isNull(),
                        eq(cotizacion.getValor()));
    }

    @Test
    void recalculoConCotizacionPrevia_auditaConElValorAnteriorYElNuevo() {
        stubJugadorExistente();
        when(rendimientoPartidoRepository.findByJugadorIdAndSemanaCalculoGreaterThanEqual(
                        any(), any()))
                .thenReturn(List.of());
        when(cotizacionHistoricaRepository.findTopByJugadorIdOrderByFechaCalculoDesc(jugadorId))
                .thenReturn(Optional.of(cotizacionAnteriorDe(new BigDecimal("150.00"))));

        CotizacionHistorica cotizacion =
                service.calcularCotizacion(jugadorId, OrigenCotizacion.AUTOMATICO, null);

        verify(auditoriaService)
                .registrar(
                        isNull(),
                        eq("RECALCULO_COTIZACION"),
                        eq("Jugador"),
                        eq(jugadorId),
                        eq(new BigDecimal("150.00")),
                        eq(cotizacion.getValor()));
    }

    // ---- recalcular ----

    @Test
    void recalcular_sinJugadoresActivos_noLlamaACalcularCotizacion() {
        when(jugadorRepository.findByEstado(EstadoJugador.ACTIVO)).thenReturn(List.of());

        service.recalcular(OrigenCotizacion.AUTOMATICO, null);

        verify(self, never()).calcularCotizacion(any(), any(), any());
    }

    @Test
    void recalcular_conJugadoresActivos_invocaACalcularCotizacionPorCadaUnoViaSelf() {
        Jugador uno = jugadorActivoCon(UUID.randomUUID());
        Jugador otro = jugadorActivoCon(UUID.randomUUID());
        when(jugadorRepository.findByEstado(EstadoJugador.ACTIVO))
                .thenReturn(List.of(JugadorEntity.desde(uno), JugadorEntity.desde(otro)));

        service.recalcular(OrigenCotizacion.AUTOMATICO, actorId);

        verify(self).calcularCotizacion(uno.getId(), OrigenCotizacion.AUTOMATICO, actorId);
        verify(self).calcularCotizacion(otro.getId(), OrigenCotizacion.AUTOMATICO, actorId);
    }

    @Test
    void recalcular_fallaEnUnJugador_noInterrumpeElCalculoDeLosDemas() {
        Jugador fallido = jugadorActivoCon(UUID.randomUUID());
        Jugador exitoso = jugadorActivoCon(UUID.randomUUID());
        when(jugadorRepository.findByEstado(EstadoJugador.ACTIVO))
                .thenReturn(List.of(JugadorEntity.desde(fallido), JugadorEntity.desde(exitoso)));
        when(self.calcularCotizacion(eq(fallido.getId()), eq(OrigenCotizacion.AUTOMATICO), any()))
                .thenThrow(new RuntimeException("fallo inesperado"));

        service.recalcular(OrigenCotizacion.AUTOMATICO, null);

        verify(self).calcularCotizacion(exitoso.getId(), OrigenCotizacion.AUTOMATICO, null);
    }

    // ---- obtenerHistorial ----

    @Test
    void obtenerHistorial_jugadorInexistente_lanzaRecursoNoEncontrado() {
        when(jugadorRepository.existsById(jugadorId)).thenReturn(false);

        assertThatThrownBy(() -> service.obtenerHistorial(jugadorId))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void obtenerHistorial_devuelveElHistorialEnOrdenCronologico() {
        when(jugadorRepository.existsById(jugadorId)).thenReturn(true);
        CotizacionHistoricaEntity primera = cotizacionAnteriorDe(new BigDecimal("100.00"));
        CotizacionHistoricaEntity segunda = cotizacionAnteriorDe(new BigDecimal("105.50"));
        when(cotizacionHistoricaRepository.findByJugadorIdOrderByFechaCalculoAsc(jugadorId))
                .thenReturn(List.of(primera, segunda));

        List<CotizacionHistorica> historial = service.obtenerHistorial(jugadorId);

        assertThat(historial).hasSize(2);
        assertThat(historial.get(0).getValor()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(historial.get(1).getValor()).isEqualByComparingTo(new BigDecimal("105.50"));
    }

    @Test
    void obtenerHistorial_sinCotizacionesPrevias_devuelveListaVacia() {
        when(jugadorRepository.existsById(jugadorId)).thenReturn(true);
        when(cotizacionHistoricaRepository.findByJugadorIdOrderByFechaCalculoAsc(jugadorId))
                .thenReturn(List.of());

        assertThat(service.obtenerHistorial(jugadorId)).isEmpty();
    }

    private void verificarPersistenciaYPunteroDelJugador(CotizacionHistorica cotizacionEsperada) {
        ArgumentCaptor<CotizacionHistoricaEntity> cotizacionCaptor =
                ArgumentCaptor.forClass(CotizacionHistoricaEntity.class);
        verify(cotizacionHistoricaRepository).save(cotizacionCaptor.capture());
        assertThat(cotizacionCaptor.getValue().getValor())
                .isEqualByComparingTo(cotizacionEsperada.getValor());

        ArgumentCaptor<JugadorEntity> jugadorCaptor = ArgumentCaptor.forClass(JugadorEntity.class);
        verify(jugadorRepository).save(jugadorCaptor.capture());
        assertThat(jugadorCaptor.getValue().getCotizacionVigenteId())
                .isEqualTo(cotizacionEsperada.getId());
    }

    private List<RendimientoPartidoEntity> historicoConMaximoEnLaSemanaActual() {
        return List.of(
                rendimientoDe(semanaHace(2), "{\"goles\":0,\"asistencias\":0}"),
                rendimientoDe(semanaHace(1), "{\"goles\":2,\"asistencias\":1}"),
                rendimientoDe(semanaActualEsperada(), "{\"goles\":4,\"asistencias\":2}"));
    }

    private List<RendimientoPartidoEntity> historicoConMinimoEnLaSemanaActual() {
        return List.of(
                rendimientoDe(semanaHace(2), "{\"goles\":2,\"asistencias\":1}"),
                rendimientoDe(semanaHace(1), "{\"goles\":4,\"asistencias\":2}"),
                rendimientoDe(semanaActualEsperada(), "{\"goles\":0,\"asistencias\":0}"));
    }

    private static RendimientoPartidoEntity rendimientoDe(String semana, String metricasJson) {
        RendimientoPartidoEntity entity = new RendimientoPartidoEntity();
        entity.setId(UUID.randomUUID());
        entity.setJugadorId(UUID.randomUUID());
        entity.setPartidoExternoId(UUID.randomUUID().toString());
        entity.setFechaPartido(LocalDate.now());
        entity.setSemanaCalculo(semana);
        entity.setMetricas(metricasJson);
        entity.setFuenteResultado(FuenteResultado.FOOTBALL_DATA);
        entity.setFechaIngesta(Instant.now());
        return entity;
    }

    private static PesoMetricaEntity pesoDe(String clave, String peso) {
        PesoMetricaEntity entity = new PesoMetricaEntity();
        entity.setClave(clave);
        entity.setPeso(new BigDecimal(peso));
        entity.setActivo(true);
        return entity;
    }

    private static CotizacionHistoricaEntity cotizacionAnteriorDe(BigDecimal valor) {
        CotizacionHistoricaEntity entity = new CotizacionHistoricaEntity();
        entity.setId(UUID.randomUUID());
        entity.setJugadorId(UUID.randomUUID());
        entity.setSemana("2026-W01");
        entity.setValor(valor);
        entity.setOrigen(OrigenCotizacion.AUTOMATICO);
        entity.setFechaCalculo(Instant.now());
        return entity;
    }

    private static String semanaActualEsperada() {
        return semanaIsoDe(LocalDate.now());
    }

    private static String semanaHace(int semanas) {
        return semanaIsoDe(LocalDate.now().minusWeeks(semanas));
    }

    private static String semanaIsoDe(LocalDate fecha) {
        int semana = fecha.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int anio = fecha.get(IsoFields.WEEK_BASED_YEAR);
        return "%04d-W%02d".formatted(anio, semana);
    }
}
