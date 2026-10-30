package com.tdetroy.valuacion.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException;
import com.tdetroy.valuacion.entity.CotizacionHistoricaEntity;
import com.tdetroy.valuacion.entity.SubastaEntity;
import com.tdetroy.valuacion.entity.TenenciaTokenEntity;
import com.tdetroy.valuacion.entity.UsuarioEntity;
import com.tdetroy.valuacion.model.CotizacionHistorica;
import com.tdetroy.valuacion.model.EstadoSubasta;
import com.tdetroy.valuacion.model.IncrementoPuja;
import com.tdetroy.valuacion.model.OrigenCotizacion;
import com.tdetroy.valuacion.model.Subasta;
import com.tdetroy.valuacion.model.TenenciaToken;
import com.tdetroy.valuacion.model.Usuario;
import com.tdetroy.valuacion.repositories.CotizacionHistoricaRepository;
import com.tdetroy.valuacion.repositories.SubastaRepository;
import com.tdetroy.valuacion.repositories.TenenciaTokenRepository;
import com.tdetroy.valuacion.repositories.UsuarioRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Cubre {@link PortfolioServiceImpl} (tasks.md T5.3, UC-11): combina tenencias con {@code cantidad
 * > 0} y su cotización vigente (o el valor inicial si el jugador nunca tuvo cálculo), calcula el
 * valor total como tenencias + saldo, y devuelve un portfolio vacío para un usuario sin tenencias.
 */
@ExtendWith(MockitoExtension.class)
class PortfolioServiceImplTest {

    private static final BigDecimal VALOR_INICIAL = new BigDecimal("100.00");
    private static final String EMAIL = "persona@example.com";
    private static final String PASSWORD_HASH = "hash-bcrypt";

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TenenciaTokenRepository tenenciaTokenRepository;
    @Mock private CotizacionHistoricaRepository cotizacionHistoricaRepository;
    @Mock private SubastaRepository subastaRepository;

    private UUID usuarioId;
    private PortfolioServiceImpl service;

    @BeforeEach
    void setUp() {
        usuarioId = UUID.randomUUID();
        service =
                new PortfolioServiceImpl(
                        usuarioRepository,
                        tenenciaTokenRepository,
                        cotizacionHistoricaRepository,
                        subastaRepository,
                        VALOR_INICIAL);
    }

    private void stubUsuarioConSaldo(BigDecimal saldo) {
        Usuario usuario = Usuario.registrar(EMAIL, PASSWORD_HASH);
        if (saldo.signum() > 0) {
            usuario.acreditarSaldo(saldo);
        }
        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.of(UsuarioEntity.desde(usuario)));
    }

    @Test
    void obtenerDe_conTenenciasYCotizacionVigente_calculaValorPorTenenciaYTotal() {
        stubUsuarioConSaldo(new BigDecimal("50.00"));
        UUID jugadorId = UUID.randomUUID();
        TenenciaToken tenencia = TenenciaToken.abrir(usuarioId, jugadorId);
        tenencia.acreditar(4);
        when(tenenciaTokenRepository.findByUsuarioIdAndCantidadGreaterThan(usuarioId, 0))
                .thenReturn(List.of(TenenciaTokenEntity.desde(tenencia)));
        CotizacionHistorica cotizacion =
                CotizacionHistorica.calcular(
                        jugadorId,
                        "2026-W10",
                        new BigDecimal("25.00"),
                        OrigenCotizacion.AUTOMATICO);
        when(cotizacionHistoricaRepository.findTopByJugadorIdOrderByFechaCalculoDesc(jugadorId))
                .thenReturn(Optional.of(CotizacionHistoricaEntity.desde(cotizacion)));

        PortfolioService.Portfolio portfolio = service.obtenerDe(usuarioId);

        assertThat(portfolio.saldoVirtual()).isEqualByComparingTo("50.00");
        assertThat(portfolio.tenencias()).hasSize(1);
        PortfolioService.TenenciaPortfolio tenenciaPortfolio = portfolio.tenencias().get(0);
        assertThat(tenenciaPortfolio.jugadorId()).isEqualTo(jugadorId);
        assertThat(tenenciaPortfolio.cantidad()).isEqualTo(4);
        assertThat(tenenciaPortfolio.cotizacionVigente()).isEqualByComparingTo("25.00");
        assertThat(tenenciaPortfolio.valor()).isEqualByComparingTo("100.00");
        // total = 100.00 (tenencia) + 50.00 (saldo)
        assertThat(portfolio.valorTotal()).isEqualByComparingTo("150.00");
    }

    @Test
    void obtenerDe_sinCotizacionPreviaDeUnJugador_usaElValorInicialComoCotizacionVigente() {
        stubUsuarioConSaldo(BigDecimal.ZERO);
        UUID jugadorId = UUID.randomUUID();
        TenenciaToken tenencia = TenenciaToken.abrir(usuarioId, jugadorId);
        tenencia.acreditar(2);
        when(tenenciaTokenRepository.findByUsuarioIdAndCantidadGreaterThan(usuarioId, 0))
                .thenReturn(List.of(TenenciaTokenEntity.desde(tenencia)));
        when(cotizacionHistoricaRepository.findTopByJugadorIdOrderByFechaCalculoDesc(jugadorId))
                .thenReturn(Optional.empty());

        PortfolioService.Portfolio portfolio = service.obtenerDe(usuarioId);

        PortfolioService.TenenciaPortfolio tenenciaPortfolio = portfolio.tenencias().get(0);
        assertThat(tenenciaPortfolio.cotizacionVigente()).isEqualByComparingTo(VALOR_INICIAL);
        assertThat(tenenciaPortfolio.valor()).isEqualByComparingTo("200.00");
        assertThat(portfolio.valorTotal()).isEqualByComparingTo("200.00");
    }

    @Test
    void obtenerDe_conVariasTenencias_sumaElValorDeTodasMasElSaldo() {
        stubUsuarioConSaldo(new BigDecimal("10.00"));
        UUID jugadorUno = UUID.randomUUID();
        UUID jugadorDos = UUID.randomUUID();
        TenenciaToken tenenciaUno = TenenciaToken.abrir(usuarioId, jugadorUno);
        tenenciaUno.acreditar(3);
        TenenciaToken tenenciaDos = TenenciaToken.abrir(usuarioId, jugadorDos);
        tenenciaDos.acreditar(5);
        when(tenenciaTokenRepository.findByUsuarioIdAndCantidadGreaterThan(usuarioId, 0))
                .thenReturn(
                        List.of(
                                TenenciaTokenEntity.desde(tenenciaUno),
                                TenenciaTokenEntity.desde(tenenciaDos)));
        when(cotizacionHistoricaRepository.findTopByJugadorIdOrderByFechaCalculoDesc(jugadorUno))
                .thenReturn(Optional.empty());
        when(cotizacionHistoricaRepository.findTopByJugadorIdOrderByFechaCalculoDesc(jugadorDos))
                .thenReturn(Optional.empty());

        PortfolioService.Portfolio portfolio = service.obtenerDe(usuarioId);

        // (3 * 100.00) + (5 * 100.00) + 10.00 saldo = 810.00
        assertThat(portfolio.tenencias()).hasSize(2);
        assertThat(portfolio.valorTotal()).isEqualByComparingTo("810.00");
    }

    @Test
    void obtenerDe_sinTenencias_devuelvePortfolioVacioConElSaldo() {
        stubUsuarioConSaldo(new BigDecimal("30.00"));
        when(tenenciaTokenRepository.findByUsuarioIdAndCantidadGreaterThan(usuarioId, 0))
                .thenReturn(List.of());

        PortfolioService.Portfolio portfolio = service.obtenerDe(usuarioId);

        assertThat(portfolio.tenencias()).isEmpty();
        assertThat(portfolio.saldoVirtual()).isEqualByComparingTo("30.00");
        assertThat(portfolio.valorTotal()).isEqualByComparingTo("30.00");
    }

    @Test
    void obtenerDe_conTokensReservadosEnOfertaP2P_losSacaDeTenenciasPeroLosSumaAlValorTotal() {
        stubUsuarioConSaldo(BigDecimal.ZERO);
        UUID jugadorParcial = UUID.randomUUID();
        TenenciaToken parcial = TenenciaToken.abrir(usuarioId, jugadorParcial);
        parcial.acreditar(5);
        parcial.reservar(2);
        TenenciaToken todaEnVenta = TenenciaToken.abrir(usuarioId, UUID.randomUUID());
        todaEnVenta.acreditar(1);
        todaEnVenta.reservar(1);
        when(tenenciaTokenRepository.findByUsuarioIdAndCantidadGreaterThan(usuarioId, 0))
                .thenReturn(
                        List.of(
                                TenenciaTokenEntity.desde(parcial),
                                TenenciaTokenEntity.desde(todaEnVenta)));
        when(cotizacionHistoricaRepository.findTopByJugadorIdOrderByFechaCalculoDesc(
                        jugadorParcial))
                .thenReturn(Optional.empty());

        PortfolioService.Portfolio portfolio = service.obtenerDe(usuarioId);

        // la tenencia con todo reservado no aparece; la parcial muestra sólo 3 disponibles
        assertThat(portfolio.tenencias()).hasSize(1);
        PortfolioService.TenenciaPortfolio tenenciaPortfolio = portfolio.tenencias().get(0);
        assertThat(tenenciaPortfolio.jugadorId()).isEqualTo(jugadorParcial);
        assertThat(tenenciaPortfolio.cantidad()).isEqualTo(3);
        // 3 × 100.00 (valor inicial)
        assertThat(tenenciaPortfolio.valor()).isEqualByComparingTo("300.00");
        // en venta: (2 + 1) × 100.00 — siguen siendo patrimonio mientras la oferta está abierta
        assertThat(portfolio.valorEnVenta()).isEqualByComparingTo("300.00");
        // total = 300.00 (disponible) + 300.00 (en venta) + 0 (saldo)
        assertThat(portfolio.valorTotal()).isEqualByComparingTo("600.00");
    }

    @Test
    void obtenerDe_comoMejorPostorDeUnaSubasta_sumaElSaldoReservadoAlValorTotal() {
        stubUsuarioConSaldo(new BigDecimal("40.00"));
        when(tenenciaTokenRepository.findByUsuarioIdAndCantidadGreaterThan(usuarioId, 0))
                .thenReturn(List.of());
        Subasta subasta =
                Subasta.crear(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        3,
                        new BigDecimal("20.00"),
                        Duration.ofMinutes(30),
                        Instant.now());
        subasta.pujar(usuarioId, IncrementoPuja.PRECIO_INICIAL, Instant.now());
        when(subastaRepository.findByMejorPostorIdAndEstado(usuarioId, EstadoSubasta.ACTIVA))
                .thenReturn(List.of(SubastaEntity.desde(subasta)));

        PortfolioService.Portfolio portfolio = service.obtenerDe(usuarioId);

        // 3 × 20.00 reservados en la puja, fuera del saldo disponible
        assertThat(portfolio.saldoEnPujas()).isEqualByComparingTo("60.00");
        assertThat(portfolio.saldoVirtual()).isEqualByComparingTo("40.00");
        assertThat(portfolio.valorTotal()).isEqualByComparingTo("100.00");
    }

    @Test
    void obtenerDe_conUsuarioInexistente_lanzaExcepcion() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerDe(usuarioId))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining(usuarioId.toString());
    }
}
