package com.tdetroy.valuacion.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tdetroy.valuacion.common.exceptions.OfertaInsuficienteException;
import com.tdetroy.valuacion.common.exceptions.OfertaNoAbiertaException;
import com.tdetroy.valuacion.common.exceptions.OfertaNoPerteneceAlVendedorException;
import com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException;
import com.tdetroy.valuacion.common.exceptions.SaldoInsuficienteException;
import com.tdetroy.valuacion.common.exceptions.TenenciaInsuficienteException;
import com.tdetroy.valuacion.entity.MovimientoEntity;
import com.tdetroy.valuacion.entity.OfertaP2PEntity;
import com.tdetroy.valuacion.entity.TenenciaTokenEntity;
import com.tdetroy.valuacion.entity.UsuarioEntity;
import com.tdetroy.valuacion.model.EstadoOferta;
import com.tdetroy.valuacion.model.Movimiento;
import com.tdetroy.valuacion.model.OfertaP2P;
import com.tdetroy.valuacion.model.TenenciaToken;
import com.tdetroy.valuacion.model.TipoMovimiento;
import com.tdetroy.valuacion.model.Usuario;
import com.tdetroy.valuacion.repositories.JugadorRepository;
import com.tdetroy.valuacion.repositories.MovimientoRepository;
import com.tdetroy.valuacion.repositories.OfertaP2PRepository;
import com.tdetroy.valuacion.repositories.TenenciaTokenRepository;
import com.tdetroy.valuacion.repositories.UsuarioRepository;
import java.math.BigDecimal;
import java.time.Instant;
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
 * Cubre {@link OfertaServiceImpl}: {@code publicar}/{@code ejecutar}/{@code cancelar} (UC-17,
 * tasks.md T6.2/T6.4/T6.5), con los casos límite de plan.md §9 (tenencia insuficiente al publicar,
 * cantidad/saldo insuficientes al ejecutar, dueño y estado al cancelar), y el vencimiento
 * automático por horas fijado libremente por el vendedor ({@code cancelarVencidas}/{@code
 * cancelarUnaVencida}).
 */
@ExtendWith(MockitoExtension.class)
class OfertaServiceImplTest {

    private static final BigDecimal PRECIO_UNITARIO = new BigDecimal("20.00");
    private static final long HORAS_LIMITE = 24L;
    private static final String EMAIL_VENDEDOR = "vendedor@example.com";
    private static final String EMAIL_COMPRADOR = "comprador@example.com";
    private static final String PASSWORD_HASH = "hash-bcrypt";

    @Mock private OfertaP2PRepository ofertaP2PRepository;
    @Mock private TenenciaTokenRepository tenenciaTokenRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private JugadorRepository jugadorRepository;
    @Mock private MovimientoRepository movimientoRepository;
    @Mock private AuditoriaService auditoriaService;
    @Mock private OfertaService self;

    private UUID vendedorId;
    private UUID compradorId;
    private UUID jugadorId;
    private OfertaServiceImpl service;

    @BeforeEach
    void setUp() {
        vendedorId = UUID.randomUUID();
        compradorId = UUID.randomUUID();
        jugadorId = UUID.randomUUID();
        service =
                new OfertaServiceImpl(
                        ofertaP2PRepository,
                        tenenciaTokenRepository,
                        usuarioRepository,
                        jugadorRepository,
                        movimientoRepository,
                        auditoriaService,
                        self);
    }

    private void stubJugadorExiste() {
        when(jugadorRepository.existsById(jugadorId)).thenReturn(true);
    }

    private void stubTenenciaVendedor(int cantidad, int cantidadReservada) {
        TenenciaToken tenencia = TenenciaToken.abrir(vendedorId, jugadorId);
        if (cantidad > 0) {
            tenencia.acreditar(cantidad);
        }
        if (cantidadReservada > 0) {
            tenencia.reservar(cantidadReservada);
        }
        when(tenenciaTokenRepository.findByUsuarioIdAndJugadorId(vendedorId, jugadorId))
                .thenReturn(Optional.of(TenenciaTokenEntity.desde(tenencia)));
    }

    private void stubSinTenenciaVendedor() {
        when(tenenciaTokenRepository.findByUsuarioIdAndJugadorId(vendedorId, jugadorId))
                .thenReturn(Optional.empty());
    }

    private void stubUsuarioConSaldo(UUID usuarioId, String email, BigDecimal saldo) {
        Usuario usuario = Usuario.registrar(email, PASSWORD_HASH);
        if (saldo.signum() > 0) {
            usuario.acreditarSaldo(saldo);
        }
        // El id de Usuario.registrar(...) es aleatorio (igual que en producción antes del primer
        // save); acá se lo fuerza a usuarioId para que coincida con la key del stub -- en
        // producción esa coincidencia la garantiza la base (el id leído es siempre el id
        // consultado).
        UsuarioEntity entity = UsuarioEntity.desde(usuario);
        entity.setId(usuarioId);
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(entity));
    }

    private OfertaP2P ofertaAbierta(int cantidadPublicada) {
        return OfertaP2P.publicar(
                vendedorId, jugadorId, cantidadPublicada, PRECIO_UNITARIO, HORAS_LIMITE);
    }

    private void stubOferta(UUID ofertaId, OfertaP2P oferta) {
        when(ofertaP2PRepository.findById(ofertaId))
                .thenReturn(Optional.of(OfertaP2PEntity.desde(oferta)));
    }

    private void stubSinTenenciaComprador() {
        when(tenenciaTokenRepository.findByUsuarioIdAndJugadorId(compradorId, jugadorId))
                .thenReturn(Optional.empty());
    }

    private void stubMovimientoGuardado() {
        when(movimientoRepository.save(any(MovimientoEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ---- publicar (UC-17, plan.md §9.1) ----

    @Test
    void publicar_conTenenciaDisponibleSuficiente_reservaYCreaOfertaAbierta() {
        stubJugadorExiste();
        stubTenenciaVendedor(10, 0);

        OfertaP2P oferta =
                service.publicar(vendedorId, jugadorId, 6, PRECIO_UNITARIO, HORAS_LIMITE);

        assertThat(oferta.getVendedorId()).isEqualTo(vendedorId);
        assertThat(oferta.getJugadorId()).isEqualTo(jugadorId);
        assertThat(oferta.getCantidadPublicada()).isEqualTo(6);
        assertThat(oferta.getCantidadDisponible()).isEqualTo(6);
        assertThat(oferta.getPrecioUnitario()).isEqualByComparingTo(PRECIO_UNITARIO);
        assertThat(oferta.getEstado()).isEqualTo(EstadoOferta.ABIERTA);

        ArgumentCaptor<TenenciaTokenEntity> tenenciaCaptor =
                ArgumentCaptor.forClass(TenenciaTokenEntity.class);
        verify(tenenciaTokenRepository).save(tenenciaCaptor.capture());
        assertThat(tenenciaCaptor.getValue().aModelo().getCantidadReservada()).isEqualTo(6);

        verify(ofertaP2PRepository).save(any(OfertaP2PEntity.class));
    }

    @Test
    void publicar_queExcedeLaTenenciaDisponible_lanzaExcepcionYNoMutaNada() {
        stubJugadorExiste();
        stubTenenciaVendedor(10, 4);

        assertThatThrownBy(
                        () ->
                                service.publicar(
                                        vendedorId, jugadorId, 7, PRECIO_UNITARIO, HORAS_LIMITE))
                .isInstanceOf(TenenciaInsuficienteException.class);

        verify(tenenciaTokenRepository, never()).save(any());
        verify(ofertaP2PRepository, never()).save(any());
    }

    @Test
    void publicar_sinTenenciaPrevia_lanzaExcepcion() {
        stubJugadorExiste();
        stubSinTenenciaVendedor();

        assertThatThrownBy(
                        () ->
                                service.publicar(
                                        vendedorId, jugadorId, 1, PRECIO_UNITARIO, HORAS_LIMITE))
                .isInstanceOf(TenenciaInsuficienteException.class);

        verify(ofertaP2PRepository, never()).save(any());
    }

    @Test
    void publicar_conJugadorInexistente_lanzaExcepcion() {
        when(jugadorRepository.existsById(jugadorId)).thenReturn(false);

        assertThatThrownBy(
                        () ->
                                service.publicar(
                                        vendedorId, jugadorId, 1, PRECIO_UNITARIO, HORAS_LIMITE))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining(jugadorId.toString());

        verifyNoInteractions(tenenciaTokenRepository, ofertaP2PRepository);
    }

    // ---- listarAbiertasDe / listarAbiertas ----

    @Test
    void listarAbiertasDe_delegaEnElRepositorioYTraduceAModelo() {
        OfertaP2P oferta = ofertaAbierta(5);
        when(ofertaP2PRepository.findByJugadorIdAndEstado(jugadorId, EstadoOferta.ABIERTA))
                .thenReturn(List.of(OfertaP2PEntity.desde(oferta)));

        var ofertas = service.listarAbiertasDe(jugadorId);

        assertThat(ofertas).hasSize(1);
        assertThat(ofertas.get(0).getId()).isEqualTo(oferta.getId());
    }

    @Test
    void listarAbiertas_delegaEnElRepositorioSinFiltrarPorJugador() {
        OfertaP2P oferta = ofertaAbierta(5);
        when(ofertaP2PRepository.findByEstado(EstadoOferta.ABIERTA))
                .thenReturn(List.of(OfertaP2PEntity.desde(oferta)));

        var ofertas = service.listarAbiertas();

        assertThat(ofertas).hasSize(1);
        assertThat(ofertas.get(0).getId()).isEqualTo(oferta.getId());
    }

    // ---- ejecutar (UC-17, plan.md §9.2) ----

    @Test
    void ejecutar_conCantidadYSaldoSuficientes_ejecutaAlPrecioFijadoPorElVendedor() {
        UUID ofertaId = UUID.randomUUID();
        stubOferta(ofertaId, ofertaAbierta(10));
        stubUsuarioConSaldo(compradorId, EMAIL_COMPRADOR, new BigDecimal("500.00"));
        stubUsuarioConSaldo(vendedorId, EMAIL_VENDEDOR, BigDecimal.ZERO);
        stubTenenciaVendedor(10, 10);
        stubSinTenenciaComprador();
        stubMovimientoGuardado();

        Movimiento movimiento = service.ejecutar(compradorId, ofertaId, 4);

        assertThat(movimiento.getTipo()).isEqualTo(TipoMovimiento.COMPRA_P2P);
        assertThat(movimiento.getUsuarioId()).isEqualTo(compradorId);
        assertThat(movimiento.getContraparteUsuarioId()).isEqualTo(vendedorId);
        assertThat(movimiento.getCantidad()).isEqualTo(4);
        assertThat(movimiento.getMontoTotal()).isEqualByComparingTo("80.00");

        ArgumentCaptor<MovimientoEntity> movimientoCaptor =
                ArgumentCaptor.forClass(MovimientoEntity.class);
        verify(movimientoRepository, times(2)).save(movimientoCaptor.capture());
        assertThat(movimientoCaptor.getAllValues())
                .extracting(m -> m.aModelo().getTipo())
                .containsExactlyInAnyOrder(TipoMovimiento.VENTA_P2P, TipoMovimiento.COMPRA_P2P);

        ArgumentCaptor<UsuarioEntity> usuarioCaptor = ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(usuarioRepository, times(2)).save(usuarioCaptor.capture());
        assertThat(usuarioCaptor.getAllValues())
                .extracting(u -> u.aModelo().getId())
                .containsExactlyInAnyOrder(compradorId, vendedorId);

        ArgumentCaptor<OfertaP2PEntity> ofertaCaptor =
                ArgumentCaptor.forClass(OfertaP2PEntity.class);
        verify(ofertaP2PRepository).save(ofertaCaptor.capture());
        assertThat(ofertaCaptor.getValue().aModelo().getCantidadDisponible()).isEqualTo(6);
        assertThat(ofertaCaptor.getValue().aModelo().getEstado()).isEqualTo(EstadoOferta.ABIERTA);

        verify(auditoriaService)
                .registrar(
                        eq(compradorId),
                        eq("EJECUCION_OFERTA_P2P"),
                        eq("OfertaP2P"),
                        eq(ofertaId),
                        isNull(),
                        any());
    }

    @Test
    void ejecutar_laCompraQueAgotaLaOferta_laDejaAgotada() {
        UUID ofertaId = UUID.randomUUID();
        stubOferta(ofertaId, ofertaAbierta(4));
        stubUsuarioConSaldo(compradorId, EMAIL_COMPRADOR, new BigDecimal("500.00"));
        stubUsuarioConSaldo(vendedorId, EMAIL_VENDEDOR, BigDecimal.ZERO);
        stubTenenciaVendedor(4, 4);
        stubSinTenenciaComprador();
        stubMovimientoGuardado();

        service.ejecutar(compradorId, ofertaId, 4);

        ArgumentCaptor<OfertaP2PEntity> ofertaCaptor =
                ArgumentCaptor.forClass(OfertaP2PEntity.class);
        verify(ofertaP2PRepository).save(ofertaCaptor.capture());
        assertThat(ofertaCaptor.getValue().aModelo().getCantidadDisponible()).isZero();
        assertThat(ofertaCaptor.getValue().aModelo().getEstado()).isEqualTo(EstadoOferta.AGOTADA);
    }

    @Test
    void ejecutar_conCantidadQueSuperaLaDisponible_lanzaExcepcionYNoMutaNada() {
        UUID ofertaId = UUID.randomUUID();
        stubOferta(ofertaId, ofertaAbierta(3));
        stubUsuarioConSaldo(compradorId, EMAIL_COMPRADOR, new BigDecimal("500.00"));

        assertThatThrownBy(() -> service.ejecutar(compradorId, ofertaId, 4))
                .isInstanceOf(OfertaInsuficienteException.class);

        verify(usuarioRepository, never()).save(any());
        verify(tenenciaTokenRepository, never()).save(any());
        verify(ofertaP2PRepository, never()).save(any());
        verifyNoInteractions(movimientoRepository, auditoriaService);
    }

    @Test
    void ejecutar_sobreOfertaYaAgotada_lanzaExcepcion() {
        UUID ofertaId = UUID.randomUUID();
        OfertaP2P agotada = ofertaAbierta(5);
        agotada.ejecutarContra(5);
        stubOferta(ofertaId, agotada);
        stubUsuarioConSaldo(compradorId, EMAIL_COMPRADOR, new BigDecimal("500.00"));

        assertThatThrownBy(() -> service.ejecutar(compradorId, ofertaId, 1))
                .isInstanceOf(OfertaInsuficienteException.class);
    }

    @Test
    void ejecutar_conSaldoInsuficienteDelComprador_lanzaExcepcionYNoMutaNada() {
        UUID ofertaId = UUID.randomUUID();
        stubOferta(ofertaId, ofertaAbierta(10));
        stubUsuarioConSaldo(compradorId, EMAIL_COMPRADOR, new BigDecimal("10.00"));

        assertThatThrownBy(() -> service.ejecutar(compradorId, ofertaId, 4))
                .isInstanceOf(SaldoInsuficienteException.class);

        verify(usuarioRepository, never()).save(any());
        verify(tenenciaTokenRepository, never()).save(any());
        verify(ofertaP2PRepository, never()).save(any());
        verifyNoInteractions(movimientoRepository, auditoriaService);
    }

    @Test
    void ejecutar_conOfertaInexistente_lanzaExcepcion() {
        UUID ofertaId = UUID.randomUUID();
        stubUsuarioConSaldo(compradorId, EMAIL_COMPRADOR, new BigDecimal("500.00"));
        when(ofertaP2PRepository.findById(ofertaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ejecutar(compradorId, ofertaId, 1))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining(ofertaId.toString());
    }

    @Test
    void ejecutar_conCompradorInexistente_lanzaExcepcion() {
        UUID ofertaId = UUID.randomUUID();
        when(usuarioRepository.findById(compradorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ejecutar(compradorId, ofertaId, 1))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining(compradorId.toString());

        verifyNoInteractions(ofertaP2PRepository);
    }

    // ---- cancelar (UC-17, plan.md §9.4) ----

    @Test
    void cancelar_conOfertaAbiertaPropia_liberaLaReservaYLaCancela() {
        UUID ofertaId = UUID.randomUUID();
        stubOferta(ofertaId, ofertaAbierta(7));
        stubTenenciaVendedor(7, 7);

        service.cancelar(vendedorId, ofertaId);

        ArgumentCaptor<TenenciaTokenEntity> tenenciaCaptor =
                ArgumentCaptor.forClass(TenenciaTokenEntity.class);
        verify(tenenciaTokenRepository).save(tenenciaCaptor.capture());
        assertThat(tenenciaCaptor.getValue().aModelo().getCantidadReservada()).isZero();

        ArgumentCaptor<OfertaP2PEntity> ofertaCaptor =
                ArgumentCaptor.forClass(OfertaP2PEntity.class);
        verify(ofertaP2PRepository).save(ofertaCaptor.capture());
        assertThat(ofertaCaptor.getValue().aModelo().getEstado()).isEqualTo(EstadoOferta.CANCELADA);
        assertThat(ofertaCaptor.getValue().aModelo().getCantidadDisponible()).isZero();
    }

    @Test
    void cancelar_conOfertaParcialmenteVendida_liberaSoloLoRemanente() {
        UUID ofertaId = UUID.randomUUID();
        OfertaP2P oferta = ofertaAbierta(10);
        oferta.ejecutarContra(3);
        stubOferta(ofertaId, oferta);
        stubTenenciaVendedor(10, 7);

        service.cancelar(vendedorId, ofertaId);

        ArgumentCaptor<TenenciaTokenEntity> tenenciaCaptor =
                ArgumentCaptor.forClass(TenenciaTokenEntity.class);
        verify(tenenciaTokenRepository).save(tenenciaCaptor.capture());
        assertThat(tenenciaCaptor.getValue().aModelo().getCantidadReservada()).isZero();
    }

    @Test
    void cancelar_conOfertaDeOtroVendedor_lanzaExcepcionYNoMutaNada() {
        UUID ofertaId = UUID.randomUUID();
        stubOferta(ofertaId, ofertaAbierta(5));

        assertThatThrownBy(() -> service.cancelar(UUID.randomUUID(), ofertaId))
                .isInstanceOf(OfertaNoPerteneceAlVendedorException.class);

        verifyNoInteractions(tenenciaTokenRepository);
        verify(ofertaP2PRepository, never()).save(any());
    }

    @Test
    void cancelar_conOfertaYaAgotada_lanzaExcepcion() {
        UUID ofertaId = UUID.randomUUID();
        OfertaP2P agotada = ofertaAbierta(5);
        agotada.ejecutarContra(5);
        stubOferta(ofertaId, agotada);

        assertThatThrownBy(() -> service.cancelar(vendedorId, ofertaId))
                .isInstanceOf(OfertaNoAbiertaException.class);

        verifyNoInteractions(tenenciaTokenRepository);
        verify(ofertaP2PRepository, never()).save(any());
    }

    @Test
    void cancelar_conOfertaYaCancelada_lanzaExcepcion() {
        UUID ofertaId = UUID.randomUUID();
        OfertaP2P cancelada = ofertaAbierta(5);
        cancelada.cancelar();
        stubOferta(ofertaId, cancelada);

        assertThatThrownBy(() -> service.cancelar(vendedorId, ofertaId))
                .isInstanceOf(OfertaNoAbiertaException.class);
    }

    @Test
    void cancelar_conOfertaInexistente_lanzaExcepcion() {
        UUID ofertaId = UUID.randomUUID();
        when(ofertaP2PRepository.findById(ofertaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelar(vendedorId, ofertaId))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining(ofertaId.toString());
    }

    // ---- cancelarVencidas / cancelarUnaVencida (UC-17: vencimiento automático) ----

    @Test
    void cancelarVencidas_delegaEnSelfPorCadaOfertaVencidaEncontrada() {
        UUID ofertaId1 = UUID.randomUUID();
        UUID ofertaId2 = UUID.randomUUID();
        OfertaP2PEntity entity1 = OfertaP2PEntity.desde(ofertaAbierta(3));
        entity1.setId(ofertaId1);
        OfertaP2PEntity entity2 = OfertaP2PEntity.desde(ofertaAbierta(5));
        entity2.setId(ofertaId2);
        when(ofertaP2PRepository.findByEstadoAndFechaVencimientoBefore(
                        eq(EstadoOferta.ABIERTA), any(Instant.class)))
                .thenReturn(List.of(entity1, entity2));

        service.cancelarVencidas();

        verify(self).cancelarUnaVencida(ofertaId1);
        verify(self).cancelarUnaVencida(ofertaId2);
    }

    @Test
    void cancelarVencidas_sinOfertasVencidas_noInvocaSelf() {
        when(ofertaP2PRepository.findByEstadoAndFechaVencimientoBefore(
                        eq(EstadoOferta.ABIERTA), any(Instant.class)))
                .thenReturn(List.of());

        service.cancelarVencidas();

        verifyNoInteractions(self);
    }

    @Test
    void cancelarVencidas_conUnaOfertaQueFallaAlCancelar_siguePorLasDemas() {
        UUID ofertaId1 = UUID.randomUUID();
        UUID ofertaId2 = UUID.randomUUID();
        OfertaP2PEntity entity1 = OfertaP2PEntity.desde(ofertaAbierta(3));
        entity1.setId(ofertaId1);
        OfertaP2PEntity entity2 = OfertaP2PEntity.desde(ofertaAbierta(5));
        entity2.setId(ofertaId2);
        when(ofertaP2PRepository.findByEstadoAndFechaVencimientoBefore(
                        eq(EstadoOferta.ABIERTA), any(Instant.class)))
                .thenReturn(List.of(entity1, entity2));
        org.mockito.Mockito.doThrow(new RuntimeException("boom"))
                .when(self)
                .cancelarUnaVencida(ofertaId1);

        service.cancelarVencidas();

        verify(self).cancelarUnaVencida(ofertaId1);
        verify(self).cancelarUnaVencida(ofertaId2);
    }

    @Test
    void cancelarUnaVencida_conOfertaAbierta_liberaLaReservaYLaCancela() {
        UUID ofertaId = UUID.randomUUID();
        stubOferta(ofertaId, ofertaAbierta(7));
        stubTenenciaVendedor(7, 7);

        service.cancelarUnaVencida(ofertaId);

        ArgumentCaptor<TenenciaTokenEntity> tenenciaCaptor =
                ArgumentCaptor.forClass(TenenciaTokenEntity.class);
        verify(tenenciaTokenRepository).save(tenenciaCaptor.capture());
        assertThat(tenenciaCaptor.getValue().aModelo().getCantidadReservada()).isZero();

        ArgumentCaptor<OfertaP2PEntity> ofertaCaptor =
                ArgumentCaptor.forClass(OfertaP2PEntity.class);
        verify(ofertaP2PRepository).save(ofertaCaptor.capture());
        assertThat(ofertaCaptor.getValue().aModelo().getEstado()).isEqualTo(EstadoOferta.CANCELADA);
    }

    @Test
    void cancelarUnaVencida_conOfertaYaNoAbierta_noHaceNada() {
        UUID ofertaId = UUID.randomUUID();
        OfertaP2P agotada = ofertaAbierta(5);
        agotada.ejecutarContra(5);
        stubOferta(ofertaId, agotada);

        service.cancelarUnaVencida(ofertaId);

        verifyNoInteractions(tenenciaTokenRepository);
        verify(ofertaP2PRepository, never()).save(any());
    }
}
