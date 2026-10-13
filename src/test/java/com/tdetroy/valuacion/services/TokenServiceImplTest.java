package com.tdetroy.valuacion.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tdetroy.valuacion.common.exceptions.EmisionMaximaSuperadaException;
import com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException;
import com.tdetroy.valuacion.common.exceptions.SaldoInsuficienteException;
import com.tdetroy.valuacion.common.exceptions.TenenciaInsuficienteException;
import com.tdetroy.valuacion.entity.CotizacionHistoricaEntity;
import com.tdetroy.valuacion.entity.JugadorEntity;
import com.tdetroy.valuacion.entity.MovimientoEntity;
import com.tdetroy.valuacion.entity.TenenciaTokenEntity;
import com.tdetroy.valuacion.entity.UsuarioEntity;
import com.tdetroy.valuacion.model.CotizacionHistorica;
import com.tdetroy.valuacion.model.Jugador;
import com.tdetroy.valuacion.model.Movimiento;
import com.tdetroy.valuacion.model.OrigenCotizacion;
import com.tdetroy.valuacion.model.TenenciaToken;
import com.tdetroy.valuacion.model.TipoMovimiento;
import com.tdetroy.valuacion.model.Usuario;
import com.tdetroy.valuacion.repositories.CotizacionHistoricaRepository;
import com.tdetroy.valuacion.repositories.JugadorRepository;
import com.tdetroy.valuacion.repositories.MovimientoRepository;
import com.tdetroy.valuacion.repositories.TenenciaTokenRepository;
import com.tdetroy.valuacion.repositories.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Cubre {@link TokenServiceImpl}: {@code comprar} (UC-09) y {@code vender} (UC-10), con los casos
 * límite de tasks.md T5.1 (saldo insuficiente, excede 100, vender tokens reservados en una oferta,
 * sin tope de concentración).
 */
@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    private static final BigDecimal VALOR_INICIAL = new BigDecimal("100.00");
    private static final String EMAIL = "persona@example.com";
    private static final String PASSWORD_HASH = "hash-bcrypt";
    private static final String NOMBRE = "Leonel Messi";
    private static final String CLUB = "Inter Miami CF";
    private static final String POSICION = "Delantero";
    private static final LocalDate FECHA_NACIMIENTO = LocalDate.of(1987, 6, 24);
    private static final String NACIONALIDAD = "Argentina";

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private JugadorRepository jugadorRepository;
    @Mock private TenenciaTokenRepository tenenciaTokenRepository;
    @Mock private CotizacionHistoricaRepository cotizacionHistoricaRepository;
    @Mock private MovimientoRepository movimientoRepository;

    private UUID usuarioId;
    private UUID jugadorId;
    private TokenServiceImpl service;

    @BeforeEach
    void setUp() {
        usuarioId = UUID.randomUUID();
        jugadorId = UUID.randomUUID();
        service =
                new TokenServiceImpl(
                        usuarioRepository,
                        jugadorRepository,
                        tenenciaTokenRepository,
                        cotizacionHistoricaRepository,
                        movimientoRepository,
                        VALOR_INICIAL);
    }

    /** Sólo la necesitan los tests que llegan a registrar el {@link Movimiento} final. */
    private void stubMovimientoGuardado() {
        when(movimientoRepository.save(any(MovimientoEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private void stubUsuarioConSaldo(BigDecimal saldo) {
        Usuario usuario = Usuario.registrar(EMAIL, PASSWORD_HASH);
        if (saldo.signum() > 0) {
            usuario.acreditarSaldo(saldo);
        }
        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.of(UsuarioEntity.desde(usuario)));
    }

    private void stubJugadorConEmision(int tokensEmitidos) {
        Jugador jugador = Jugador.darAlta(NOMBRE, CLUB, POSICION, FECHA_NACIMIENTO, NACIONALIDAD);
        if (tokensEmitidos > 0) {
            jugador.emitirTokens(tokensEmitidos);
        }
        when(jugadorRepository.findById(jugadorId))
                .thenReturn(Optional.of(JugadorEntity.desde(jugador)));
    }

    private void stubSinCotizacionPrevia() {
        when(cotizacionHistoricaRepository.findTopByJugadorIdOrderByFechaCalculoDesc(jugadorId))
                .thenReturn(Optional.empty());
    }

    private void stubCotizacionVigente(BigDecimal valor) {
        CotizacionHistorica cotizacion =
                CotizacionHistorica.calcular(
                        jugadorId, "2026-W10", valor, OrigenCotizacion.AUTOMATICO);
        when(cotizacionHistoricaRepository.findTopByJugadorIdOrderByFechaCalculoDesc(jugadorId))
                .thenReturn(Optional.of(CotizacionHistoricaEntity.desde(cotizacion)));
    }

    private void stubSinTenenciaPrevia() {
        when(tenenciaTokenRepository.findByUsuarioIdAndJugadorId(usuarioId, jugadorId))
                .thenReturn(Optional.empty());
    }

    private void stubTenenciaConReserva(int cantidad, int cantidadReservada) {
        TenenciaToken tenencia = TenenciaToken.abrir(usuarioId, jugadorId);
        if (cantidad > 0) {
            tenencia.acreditar(cantidad);
        }
        if (cantidadReservada > 0) {
            tenencia.reservar(cantidadReservada);
        }
        when(tenenciaTokenRepository.findByUsuarioIdAndJugadorId(usuarioId, jugadorId))
                .thenReturn(Optional.of(TenenciaTokenEntity.desde(tenencia)));
    }

    // ---- comprar (UC-09) ----

    @Test
    void comprar_conSaldoYTokensDisponibles_debitaEmiteAcreditaYRegistraMovimiento() {
        stubUsuarioConSaldo(new BigDecimal("500.00"));
        stubJugadorConEmision(0);
        stubCotizacionVigente(new BigDecimal("50.00"));
        stubSinTenenciaPrevia();
        stubMovimientoGuardado();

        Movimiento movimiento = service.comprar(usuarioId, jugadorId, 3);

        assertThat(movimiento.getTipo()).isEqualTo(TipoMovimiento.COMPRA_SISTEMA);
        assertThat(movimiento.getUsuarioId()).isEqualTo(usuarioId);
        assertThat(movimiento.getJugadorId()).isEqualTo(jugadorId);
        assertThat(movimiento.getCantidad()).isEqualTo(3);
        assertThat(movimiento.getPrecioUnitario()).isEqualByComparingTo("50.00");
        assertThat(movimiento.getMontoTotal()).isEqualByComparingTo("150.00");
        assertThat(movimiento.getContraparteUsuarioId()).isNull();

        ArgumentCaptor<UsuarioEntity> usuarioCaptor = ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        assertThat(usuarioCaptor.getValue().aModelo().getSaldoVirtual())
                .isEqualByComparingTo("350.00");

        ArgumentCaptor<JugadorEntity> jugadorCaptor = ArgumentCaptor.forClass(JugadorEntity.class);
        verify(jugadorRepository).save(jugadorCaptor.capture());
        assertThat(jugadorCaptor.getValue().aModelo().getTokensEmitidos()).isEqualTo(3);

        ArgumentCaptor<TenenciaTokenEntity> tenenciaCaptor =
                ArgumentCaptor.forClass(TenenciaTokenEntity.class);
        verify(tenenciaTokenRepository).save(tenenciaCaptor.capture());
        assertThat(tenenciaCaptor.getValue().aModelo().getCantidad()).isEqualTo(3);
    }

    @Test
    void comprar_sinCotizacionPrevia_usaElValorInicialComoCotizacionVigente() {
        stubUsuarioConSaldo(new BigDecimal("500.00"));
        stubJugadorConEmision(0);
        stubSinCotizacionPrevia();
        stubSinTenenciaPrevia();
        stubMovimientoGuardado();

        Movimiento movimiento = service.comprar(usuarioId, jugadorId, 2);

        assertThat(movimiento.getPrecioUnitario()).isEqualByComparingTo(VALOR_INICIAL);
        assertThat(movimiento.getMontoTotal()).isEqualByComparingTo("200.00");
    }

    @Test
    void comprar_conSaldoInsuficiente_lanzaExcepcionYNoMutaNada() {
        stubUsuarioConSaldo(new BigDecimal("10.00"));
        stubJugadorConEmision(0);
        stubCotizacionVigente(new BigDecimal("50.00"));

        assertThatThrownBy(() -> service.comprar(usuarioId, jugadorId, 1))
                .isInstanceOf(SaldoInsuficienteException.class);

        verify(usuarioRepository, never()).save(any());
        verify(jugadorRepository, never()).save(any());
        verifyNoInteractions(tenenciaTokenRepository, movimientoRepository);
    }

    @Test
    void comprar_queExcedeLosTokensDisponibles_lanzaExcepcionYNoMutaNada() {
        stubUsuarioConSaldo(new BigDecimal("100000.00"));
        stubJugadorConEmision(95);
        stubCotizacionVigente(new BigDecimal("10.00"));

        assertThatThrownBy(() -> service.comprar(usuarioId, jugadorId, 10))
                .isInstanceOf(EmisionMaximaSuperadaException.class);

        verify(usuarioRepository, never()).save(any());
        verify(jugadorRepository, never()).save(any());
        verifyNoInteractions(tenenciaTokenRepository, movimientoRepository);
    }

    @Test
    void comprar_conUsuarioInexistente_lanzaExcepcion() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.comprar(usuarioId, jugadorId, 1))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining(usuarioId.toString());

        verifyNoInteractions(jugadorRepository, tenenciaTokenRepository, movimientoRepository);
    }

    @Test
    void comprar_conJugadorInexistente_lanzaExcepcion() {
        stubUsuarioConSaldo(new BigDecimal("500.00"));
        when(jugadorRepository.findById(jugadorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.comprar(usuarioId, jugadorId, 1))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining(jugadorId.toString());

        verifyNoInteractions(tenenciaTokenRepository, movimientoRepository);
    }

    @Test
    void comprar_conCantidadCero_lanzaExcepcionYNoMutaNada() {
        stubUsuarioConSaldo(new BigDecimal("500.00"));
        stubJugadorConEmision(0);
        stubCotizacionVigente(new BigDecimal("50.00"));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.comprar(usuarioId, jugadorId, 0));

        verify(usuarioRepository, never()).save(any());
        verify(jugadorRepository, never()).save(any());
        verifyNoInteractions(tenenciaTokenRepository, movimientoRepository);
    }

    @Test
    void comprar_sinTopeDeConcentracion_permiteAcumularHastaElMaximoDelJugador() {
        stubUsuarioConSaldo(new BigDecimal("100000.00"));
        stubJugadorConEmision(60);
        stubCotizacionVigente(new BigDecimal("10.00"));
        stubTenenciaConReserva(60, 0);
        stubMovimientoGuardado();

        Movimiento movimiento = service.comprar(usuarioId, jugadorId, 40);

        assertThat(movimiento.getCantidad()).isEqualTo(40);
        ArgumentCaptor<TenenciaTokenEntity> tenenciaCaptor =
                ArgumentCaptor.forClass(TenenciaTokenEntity.class);
        verify(tenenciaTokenRepository).save(tenenciaCaptor.capture());
        assertThat(tenenciaCaptor.getValue().aModelo().getCantidad()).isEqualTo(100);

        ArgumentCaptor<JugadorEntity> jugadorCaptor = ArgumentCaptor.forClass(JugadorEntity.class);
        verify(jugadorRepository).save(jugadorCaptor.capture());
        assertThat(jugadorCaptor.getValue().aModelo().getTokensEmitidos()).isEqualTo(100);
    }

    // ---- vender (UC-10) ----

    @Test
    void vender_conTenenciaDisponibleSuficiente_debitaAcreditaLiberaYRegistraMovimiento() {
        stubUsuarioConSaldo(BigDecimal.ZERO);
        stubJugadorConEmision(10);
        stubTenenciaConReserva(10, 0);
        stubCotizacionVigente(new BigDecimal("20.00"));
        stubMovimientoGuardado();

        Movimiento movimiento = service.vender(usuarioId, jugadorId, 4);

        assertThat(movimiento.getTipo()).isEqualTo(TipoMovimiento.VENTA_SISTEMA);
        assertThat(movimiento.getCantidad()).isEqualTo(4);
        assertThat(movimiento.getPrecioUnitario()).isEqualByComparingTo("20.00");
        assertThat(movimiento.getMontoTotal()).isEqualByComparingTo("80.00");

        ArgumentCaptor<TenenciaTokenEntity> tenenciaCaptor =
                ArgumentCaptor.forClass(TenenciaTokenEntity.class);
        verify(tenenciaTokenRepository).save(tenenciaCaptor.capture());
        assertThat(tenenciaCaptor.getValue().aModelo().getCantidad()).isEqualTo(6);

        ArgumentCaptor<UsuarioEntity> usuarioCaptor = ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        assertThat(usuarioCaptor.getValue().aModelo().getSaldoVirtual())
                .isEqualByComparingTo("80.00");

        ArgumentCaptor<JugadorEntity> jugadorCaptor = ArgumentCaptor.forClass(JugadorEntity.class);
        verify(jugadorRepository).save(jugadorCaptor.capture());
        assertThat(jugadorCaptor.getValue().aModelo().getTokensEmitidos()).isEqualTo(6);
    }

    @Test
    void vender_conTenenciaReservadaEnUnaOferta_lanzaExcepcionYNoMutaNada() {
        stubUsuarioConSaldo(BigDecimal.ZERO);
        stubJugadorConEmision(10);
        stubTenenciaConReserva(10, 10);

        assertThatThrownBy(() -> service.vender(usuarioId, jugadorId, 5))
                .isInstanceOf(TenenciaInsuficienteException.class);

        verify(usuarioRepository, never()).save(any());
        verify(jugadorRepository, never()).save(any());
        verify(tenenciaTokenRepository, never()).save(any());
        verifyNoInteractions(cotizacionHistoricaRepository, movimientoRepository);
    }

    @Test
    void vender_conTenenciaInsuficiente_lanzaExcepcionYNoMutaNada() {
        stubUsuarioConSaldo(BigDecimal.ZERO);
        stubJugadorConEmision(2);
        stubTenenciaConReserva(2, 0);

        assertThatThrownBy(() -> service.vender(usuarioId, jugadorId, 5))
                .isInstanceOf(TenenciaInsuficienteException.class);

        verify(usuarioRepository, never()).save(any());
        verify(jugadorRepository, never()).save(any());
        verify(tenenciaTokenRepository, never()).save(any());
        verifyNoInteractions(cotizacionHistoricaRepository, movimientoRepository);
    }

    @Test
    void vender_sinTenenciaAbierta_lanzaExcepcionYNoMutaNada() {
        stubUsuarioConSaldo(BigDecimal.ZERO);
        stubJugadorConEmision(0);
        stubSinTenenciaPrevia();

        assertThatThrownBy(() -> service.vender(usuarioId, jugadorId, 1))
                .isInstanceOf(TenenciaInsuficienteException.class);

        verifyNoInteractions(cotizacionHistoricaRepository, movimientoRepository);
        verify(tenenciaTokenRepository, never()).save(any());
    }

    @Test
    void vender_conUsuarioInexistente_lanzaExcepcion() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.vender(usuarioId, jugadorId, 1))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining(usuarioId.toString());

        verifyNoInteractions(jugadorRepository, tenenciaTokenRepository, movimientoRepository);
    }

    @Test
    void vender_conJugadorInexistente_lanzaExcepcion() {
        stubUsuarioConSaldo(BigDecimal.ZERO);
        when(jugadorRepository.findById(jugadorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.vender(usuarioId, jugadorId, 1))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining(jugadorId.toString());

        verifyNoInteractions(tenenciaTokenRepository, movimientoRepository);
    }

    @Test
    void vender_conCantidadCero_lanzaExcepcionYNoMutaNada() {
        stubUsuarioConSaldo(BigDecimal.ZERO);
        stubJugadorConEmision(10);
        stubTenenciaConReserva(10, 0);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.vender(usuarioId, jugadorId, 0));

        verify(usuarioRepository, never()).save(any());
        verify(jugadorRepository, never()).save(any());
        verify(tenenciaTokenRepository, never()).save(any());
        verifyNoInteractions(cotizacionHistoricaRepository, movimientoRepository);
    }
}
