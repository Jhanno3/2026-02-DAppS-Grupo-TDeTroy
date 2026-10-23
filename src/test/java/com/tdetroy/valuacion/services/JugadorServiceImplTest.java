package com.tdetroy.valuacion.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException;
import com.tdetroy.valuacion.entity.CotizacionHistoricaEntity;
import com.tdetroy.valuacion.entity.JugadorEntity;
import com.tdetroy.valuacion.entity.MovimientoEntity;
import com.tdetroy.valuacion.entity.OfertaP2PEntity;
import com.tdetroy.valuacion.entity.TenenciaTokenEntity;
import com.tdetroy.valuacion.entity.UsuarioEntity;
import com.tdetroy.valuacion.model.CotizacionHistorica;
import com.tdetroy.valuacion.model.EstadoJugador;
import com.tdetroy.valuacion.model.EstadoOferta;
import com.tdetroy.valuacion.model.Jugador;
import com.tdetroy.valuacion.model.OfertaP2P;
import com.tdetroy.valuacion.model.OrigenCotizacion;
import com.tdetroy.valuacion.model.TenenciaToken;
import com.tdetroy.valuacion.model.TipoMovimiento;
import com.tdetroy.valuacion.model.Usuario;
import com.tdetroy.valuacion.repositories.CotizacionHistoricaRepository;
import com.tdetroy.valuacion.repositories.JugadorRepository;
import com.tdetroy.valuacion.repositories.MovimientoRepository;
import com.tdetroy.valuacion.repositories.OfertaP2PRepository;
import com.tdetroy.valuacion.repositories.TenenciaTokenRepository;
import com.tdetroy.valuacion.repositories.UsuarioRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Cubre {@link JugadorServiceImpl}: {@code listar}/{@code obtenerPorId} (UC-07), {@code darAlta}
 * (UC-03), {@code editar} (UC-04, nunca toca cotización ni límite de tokens) y {@code darDeBaja}
 * (UC-15, tasks.md T6.6: cancelación de ofertas abiertas + compensación de tenencias, con y sin
 * cada una).
 */
@ExtendWith(MockitoExtension.class)
class JugadorServiceImplTest {

    private static final BigDecimal VALOR_INICIAL = new BigDecimal("100.00");
    private static final String NOMBRE = "Leonel Messi";
    private static final String CLUB = "Inter Miami CF";
    private static final String POSICION = "Delantero";
    private static final LocalDate FECHA_NACIMIENTO = LocalDate.of(1987, 6, 24);
    private static final String NACIONALIDAD = "Argentina";
    private static final String LIGA = "Major League Soccer";
    private static final String PASSWORD_HASH = "hash-bcrypt";

    @Mock private JugadorRepository jugadorRepository;
    @Mock private OfertaP2PRepository ofertaP2PRepository;
    @Mock private TenenciaTokenRepository tenenciaTokenRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CotizacionHistoricaRepository cotizacionHistoricaRepository;
    @Mock private MovimientoRepository movimientoRepository;
    @Mock private AuditoriaService auditoriaService;

    private JugadorServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new JugadorServiceImpl(
                        jugadorRepository,
                        ofertaP2PRepository,
                        tenenciaTokenRepository,
                        usuarioRepository,
                        cotizacionHistoricaRepository,
                        movimientoRepository,
                        auditoriaService,
                        VALOR_INICIAL);
    }

    // ---- listar / obtenerPorId (UC-07) ----

    @Test
    void listar_delegaEnElRepositorioYDevuelveElCatalogoCompleto() {
        Jugador jugador =
                Jugador.darAlta(NOMBRE, CLUB, POSICION, FECHA_NACIMIENTO, NACIONALIDAD, LIGA);
        when(jugadorRepository.findAll()).thenReturn(List.of(JugadorEntity.desde(jugador)));

        assertThat(service.listar()).extracting(Jugador::getId).containsExactly(jugador.getId());
    }

    @Test
    void obtenerPorId_conJugadorExistente_loDevuelve() {
        UUID jugadorId = UUID.randomUUID();
        Jugador jugador =
                Jugador.darAlta(NOMBRE, CLUB, POSICION, FECHA_NACIMIENTO, NACIONALIDAD, LIGA);
        when(jugadorRepository.findById(jugadorId))
                .thenReturn(Optional.of(JugadorEntity.desde(jugador)));

        assertThat(service.obtenerPorId(jugadorId).getId()).isEqualTo(jugador.getId());
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
        when(jugadorRepository.save(any(JugadorEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Jugador creado =
                service.darAlta(
                        NOMBRE, CLUB, POSICION, FECHA_NACIMIENTO, NACIONALIDAD, LIGA, actorId);

        assertThat(creado.getNombre()).isEqualTo(NOMBRE);
        assertThat(creado.getEstado()).isEqualTo(EstadoJugador.ACTIVO);
        assertThat(creado.getTokensEmitidos()).isZero();
        verify(jugadorRepository).save(any(JugadorEntity.class));
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
                                        LIGA,
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
                        "Nombre Viejo",
                        "Club Viejo",
                        "Arquero",
                        FECHA_NACIMIENTO,
                        "Brasil",
                        "Brasileirao");
        jugador.emitirTokens(40);
        when(jugadorRepository.findById(jugadorId))
                .thenReturn(Optional.of(JugadorEntity.desde(jugador)));

        Jugador editado =
                service.editar(
                        jugadorId,
                        NOMBRE,
                        CLUB,
                        POSICION,
                        FECHA_NACIMIENTO,
                        NACIONALIDAD,
                        LIGA,
                        actorId);

        assertThat(editado.getNombre()).isEqualTo(NOMBRE);
        assertThat(editado.getClub()).isEqualTo(CLUB);
        assertThat(editado.getPosicion()).isEqualTo(POSICION);
        assertThat(editado.getNacionalidad()).isEqualTo(NACIONALIDAD);
        assertThat(editado.getLiga()).isEqualTo(LIGA);
        assertThat(editado.getEstado()).isEqualTo(EstadoJugador.ACTIVO);
        assertThat(editado.getTokensEmitidos()).isEqualTo(40);
        assertThat(editado.getCotizacionVigenteId()).isNull();

        verify(jugadorRepository).save(any(JugadorEntity.class));
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
                                        LIGA,
                                        UUID.randomUUID()))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining(jugadorId.toString());

        verifyNoInteractions(auditoriaService);
    }

    @Test
    void editar_conClubBlanco_lanzaExcepcionYNoAuditaNiMutaElJugador() {
        UUID jugadorId = UUID.randomUUID();
        Jugador jugador =
                Jugador.darAlta(NOMBRE, CLUB, POSICION, FECHA_NACIMIENTO, NACIONALIDAD, LIGA);
        when(jugadorRepository.findById(jugadorId))
                .thenReturn(Optional.of(JugadorEntity.desde(jugador)));

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
                                        LIGA,
                                        UUID.randomUUID()));

        assertThat(jugador.getClub()).isEqualTo(CLUB);
        verify(jugadorRepository, never()).save(any());
        verifyNoInteractions(auditoriaService);
    }

    // ---- darDeBaja (UC-15, tasks.md T6.6) ----

    private void stubJugadorActivo(UUID jugadorId) {
        Jugador jugador =
                Jugador.darAlta(NOMBRE, CLUB, POSICION, FECHA_NACIMIENTO, NACIONALIDAD, LIGA);
        when(jugadorRepository.findById(jugadorId))
                .thenReturn(Optional.of(JugadorEntity.desde(jugador)));
    }

    private void stubSinOfertasAbiertas(UUID jugadorId) {
        when(ofertaP2PRepository.findByJugadorIdAndEstado(jugadorId, EstadoOferta.ABIERTA))
                .thenReturn(List.of());
    }

    private void stubSinTenencias(UUID jugadorId) {
        when(tenenciaTokenRepository.findByJugadorIdAndCantidadGreaterThan(jugadorId, 0))
                .thenReturn(List.of());
    }

    private void stubCotizacionVigente(UUID jugadorId, BigDecimal valor) {
        CotizacionHistorica cotizacion =
                CotizacionHistorica.calcular(
                        jugadorId, "2026-W10", valor, OrigenCotizacion.AUTOMATICO);
        when(cotizacionHistoricaRepository.findTopByJugadorIdOrderByFechaCalculoDesc(jugadorId))
                .thenReturn(Optional.of(CotizacionHistoricaEntity.desde(cotizacion)));
    }

    private void stubSinCotizacionPrevia(UUID jugadorId) {
        when(cotizacionHistoricaRepository.findTopByJugadorIdOrderByFechaCalculoDesc(jugadorId))
                .thenReturn(Optional.empty());
    }

    private void stubTenenciaDe(UUID usuarioId, UUID jugadorId, int cantidad) {
        TenenciaToken tenencia = TenenciaToken.abrir(usuarioId, jugadorId);
        tenencia.acreditar(cantidad);
        when(tenenciaTokenRepository.findByJugadorIdAndCantidadGreaterThan(jugadorId, 0))
                .thenReturn(List.of(TenenciaTokenEntity.desde(tenencia)));
    }

    private void stubUsuarioConSaldo(UUID usuarioId, BigDecimal saldo) {
        Usuario usuario = Usuario.registrar("usuario-" + usuarioId + "@example.com", PASSWORD_HASH);
        if (saldo.signum() > 0) {
            usuario.acreditarSaldo(saldo);
        }
        // El id de Usuario.registrar(...) es aleatorio; se lo fuerza a usuarioId para que
        // coincida con la key del stub -- en producción esa coincidencia la garantiza la base.
        UsuarioEntity entity = UsuarioEntity.desde(usuario);
        entity.setId(usuarioId);
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(entity));
    }

    private void stubMovimientoGuardado() {
        when(movimientoRepository.save(any(MovimientoEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void darDeBaja_conJugadorInexistente_lanzaExcepcion() {
        UUID jugadorId = UUID.randomUUID();
        when(jugadorRepository.findById(jugadorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.darDeBaja(jugadorId, UUID.randomUUID()))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining(jugadorId.toString());

        verifyNoInteractions(
                ofertaP2PRepository,
                tenenciaTokenRepository,
                usuarioRepository,
                movimientoRepository);
    }

    @Test
    void darDeBaja_sinOfertasNiTenencias_marcaInactivoYAudita() {
        UUID jugadorId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        stubJugadorActivo(jugadorId);
        stubSinOfertasAbiertas(jugadorId);
        stubSinTenencias(jugadorId);
        stubCotizacionVigente(jugadorId, new BigDecimal("50.00"));

        Jugador jugador = service.darDeBaja(jugadorId, actorId);

        assertThat(jugador.getEstado()).isEqualTo(EstadoJugador.INACTIVO);
        verify(jugadorRepository).save(any(JugadorEntity.class));
        verifyNoInteractions(usuarioRepository, movimientoRepository);
        verify(auditoriaService)
                .registrar(
                        eq(actorId),
                        eq("BAJA_JUGADOR"),
                        eq("Jugador"),
                        eq(jugadorId),
                        isNull(),
                        any());
    }

    @Test
    void darDeBaja_conTenenciaSinOfertas_compensaAlUsuarioYRegistraMovimiento() {
        UUID jugadorId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        stubJugadorActivo(jugadorId);
        stubSinOfertasAbiertas(jugadorId);
        stubTenenciaDe(usuarioId, jugadorId, 7);
        stubUsuarioConSaldo(usuarioId, BigDecimal.ZERO);
        stubCotizacionVigente(jugadorId, new BigDecimal("20.00"));
        stubMovimientoGuardado();

        service.darDeBaja(jugadorId, UUID.randomUUID());

        ArgumentCaptor<TenenciaTokenEntity> tenenciaCaptor =
                ArgumentCaptor.forClass(TenenciaTokenEntity.class);
        verify(tenenciaTokenRepository).save(tenenciaCaptor.capture());
        assertThat(tenenciaCaptor.getValue().aModelo().getCantidad()).isZero();

        ArgumentCaptor<UsuarioEntity> usuarioCaptor = ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        assertThat(usuarioCaptor.getValue().aModelo().getSaldoVirtual())
                .isEqualByComparingTo("140.00");

        ArgumentCaptor<MovimientoEntity> movimientoCaptor =
                ArgumentCaptor.forClass(MovimientoEntity.class);
        verify(movimientoRepository).save(movimientoCaptor.capture());
        assertThat(movimientoCaptor.getValue().aModelo().getTipo())
                .isEqualTo(TipoMovimiento.COMPENSACION_BAJA);
        assertThat(movimientoCaptor.getValue().aModelo().getCantidad()).isEqualTo(7);
        assertThat(movimientoCaptor.getValue().aModelo().getMontoTotal())
                .isEqualByComparingTo("140.00");
    }

    @Test
    void darDeBaja_sinCotizacionPrevia_compensaAlValorInicial() {
        UUID jugadorId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        stubJugadorActivo(jugadorId);
        stubSinOfertasAbiertas(jugadorId);
        stubTenenciaDe(usuarioId, jugadorId, 2);
        stubUsuarioConSaldo(usuarioId, BigDecimal.ZERO);
        stubSinCotizacionPrevia(jugadorId);
        stubMovimientoGuardado();

        service.darDeBaja(jugadorId, UUID.randomUUID());

        ArgumentCaptor<MovimientoEntity> movimientoCaptor =
                ArgumentCaptor.forClass(MovimientoEntity.class);
        verify(movimientoRepository).save(movimientoCaptor.capture());
        assertThat(movimientoCaptor.getValue().aModelo().getPrecioUnitario())
                .isEqualByComparingTo(VALOR_INICIAL);
        assertThat(movimientoCaptor.getValue().aModelo().getMontoTotal())
                .isEqualByComparingTo("200.00");
    }

    @Test
    void darDeBaja_conOfertaAbierta_laCancelaReintegraYCompensaLaTenenciaCompleta() {
        UUID jugadorId = UUID.randomUUID();
        UUID vendedorId = UUID.randomUUID();
        UUID ofertaId = UUID.randomUUID();
        stubJugadorActivo(jugadorId);

        // El vendedor tiene 10 en tenencia, 6 reservados en la oferta publicada.
        TenenciaToken tenenciaVendedor = TenenciaToken.abrir(vendedorId, jugadorId);
        tenenciaVendedor.acreditar(10);
        tenenciaVendedor.reservar(6);
        when(tenenciaTokenRepository.findByUsuarioIdAndJugadorId(vendedorId, jugadorId))
                .thenReturn(Optional.of(TenenciaTokenEntity.desde(tenenciaVendedor)));

        OfertaP2P oferta =
                OfertaP2P.publicar(
                        vendedorId,
                        jugadorId,
                        6,
                        new BigDecimal("50.00"),
                        Instant.parse("2026-02-15T10:00:00Z"),
                        Instant.parse("2026-02-16T10:00:00Z"));
        when(ofertaP2PRepository.findByJugadorIdAndEstado(jugadorId, EstadoOferta.ABIERTA))
                .thenReturn(List.of(OfertaP2PEntity.desde(oferta)));
        when(ofertaP2PRepository.findById(oferta.getId()))
                .thenReturn(Optional.of(OfertaP2PEntity.desde(oferta)));

        // La tenencia a compensar (findByJugadorIdAndCantidadGreaterThan) ya refleja los 10
        // originales -- la cancelación de la oferta sólo libera la reserva, nunca cambia
        // `cantidad`, así que esta consulta separada (simulando una relectura fresca de la fila
        // ya persistida con la reserva liberada) sigue viendo cantidad=10.
        stubTenenciaDe(vendedorId, jugadorId, 10);
        stubUsuarioConSaldo(vendedorId, BigDecimal.ZERO);
        stubCotizacionVigente(jugadorId, new BigDecimal("15.00"));
        stubMovimientoGuardado();

        service.darDeBaja(jugadorId, UUID.randomUUID());

        ArgumentCaptor<OfertaP2PEntity> ofertaCaptor =
                ArgumentCaptor.forClass(OfertaP2PEntity.class);
        verify(ofertaP2PRepository).save(ofertaCaptor.capture());
        assertThat(ofertaCaptor.getValue().aModelo().getEstado()).isEqualTo(EstadoOferta.CANCELADA);
        assertThat(ofertaCaptor.getValue().aModelo().getCantidadDisponible()).isZero();

        ArgumentCaptor<TenenciaTokenEntity> tenenciaCaptor =
                ArgumentCaptor.forClass(TenenciaTokenEntity.class);
        verify(tenenciaTokenRepository, times(2)).save(tenenciaCaptor.capture());
        TenenciaToken tenenciaTrasLiberar = tenenciaCaptor.getAllValues().get(0).aModelo();
        assertThat(tenenciaTrasLiberar.getCantidadReservada()).isZero();
        TenenciaToken tenenciaTrasCompensar = tenenciaCaptor.getAllValues().get(1).aModelo();
        assertThat(tenenciaTrasCompensar.getCantidad()).isZero();

        ArgumentCaptor<MovimientoEntity> movimientoCaptor =
                ArgumentCaptor.forClass(MovimientoEntity.class);
        verify(movimientoRepository).save(movimientoCaptor.capture());
        assertThat(movimientoCaptor.getValue().aModelo().getCantidad()).isEqualTo(10);
        assertThat(movimientoCaptor.getValue().aModelo().getMontoTotal())
                .isEqualByComparingTo("150.00");
    }
}
