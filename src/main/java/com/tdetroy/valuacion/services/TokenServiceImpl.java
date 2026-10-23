package com.tdetroy.valuacion.services;

import com.tdetroy.valuacion.common.Monetario;
import com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException;
import com.tdetroy.valuacion.entity.CotizacionHistoricaEntity;
import com.tdetroy.valuacion.entity.JugadorEntity;
import com.tdetroy.valuacion.entity.MovimientoEntity;
import com.tdetroy.valuacion.entity.TenenciaTokenEntity;
import com.tdetroy.valuacion.entity.UsuarioEntity;
import com.tdetroy.valuacion.model.Jugador;
import com.tdetroy.valuacion.model.Movimiento;
import com.tdetroy.valuacion.model.TenenciaToken;
import com.tdetroy.valuacion.model.TipoMovimiento;
import com.tdetroy.valuacion.model.Usuario;
import com.tdetroy.valuacion.repositories.CotizacionHistoricaRepository;
import com.tdetroy.valuacion.repositories.JugadorRepository;
import com.tdetroy.valuacion.repositories.MovimientoRepository;
import com.tdetroy.valuacion.repositories.TenenciaTokenRepository;
import com.tdetroy.valuacion.repositories.UsuarioRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** {@inheritDoc} */
@Service
public class TokenServiceImpl implements TokenService {

    private static final String ENTIDAD_USUARIO = "Usuario";
    private static final String ENTIDAD_JUGADOR = "Jugador";

    private final UsuarioRepository usuarioRepository;
    private final JugadorRepository jugadorRepository;
    private final TenenciaTokenRepository tenenciaTokenRepository;
    private final CotizacionHistoricaRepository cotizacionHistoricaRepository;
    private final MovimientoRepository movimientoRepository;
    private final BigDecimal valorInicial;
    private final BigDecimal descuentoVentaSistema;

    public TokenServiceImpl(
            UsuarioRepository usuarioRepository,
            JugadorRepository jugadorRepository,
            TenenciaTokenRepository tenenciaTokenRepository,
            CotizacionHistoricaRepository cotizacionHistoricaRepository,
            MovimientoRepository movimientoRepository,
            @Value("${app.cotizacion.valor-inicial}") BigDecimal valorInicial,
            @Value("${app.token.descuento-venta-sistema}") BigDecimal descuentoVentaSistema) {
        this.usuarioRepository = usuarioRepository;
        this.jugadorRepository = jugadorRepository;
        this.tenenciaTokenRepository = tenenciaTokenRepository;
        this.cotizacionHistoricaRepository = cotizacionHistoricaRepository;
        this.movimientoRepository = movimientoRepository;
        this.valorInicial = valorInicial;
        this.descuentoVentaSistema = descuentoVentaSistema;
    }

    @Override
    @Transactional
    public Movimiento comprar(UUID usuarioId, UUID jugadorId, int cantidad) {
        Usuario usuario = obtenerUsuario(usuarioId);
        Jugador jugador = obtenerJugador(jugadorId);
        BigDecimal cotizacionVigente = cotizacionVigenteDe(jugadorId);
        BigDecimal costoTotal = costoDe(cantidad, cotizacionVigente);

        usuario.debitarSaldo(costoTotal);
        jugador.emitirTokens(cantidad);
        TenenciaToken tenencia = obtenerOAbrirTenencia(usuarioId, jugadorId);
        tenencia.acreditar(cantidad);

        usuarioRepository.save(UsuarioEntity.desde(usuario));
        jugadorRepository.save(JugadorEntity.desde(jugador));
        tenenciaTokenRepository.save(TenenciaTokenEntity.desde(tenencia));

        return registrarMovimiento(
                usuarioId,
                jugadorId,
                TipoMovimiento.COMPRA_SISTEMA,
                cantidad,
                cotizacionVigente,
                costoTotal);
    }

    @Override
    @Transactional
    public Movimiento vender(UUID usuarioId, UUID jugadorId, int cantidad) {
        Usuario usuario = obtenerUsuario(usuarioId);
        Jugador jugador = obtenerJugador(jugadorId);
        TenenciaToken tenencia = obtenerOAbrirTenencia(usuarioId, jugadorId);

        tenencia.debitar(cantidad);
        BigDecimal cotizacionVigente = cotizacionVigenteDe(jugadorId);
        BigDecimal precioVenta = precioVentaSistemaDe(cotizacionVigente);
        BigDecimal montoTotal = costoDe(cantidad, precioVenta);
        usuario.acreditarSaldo(montoTotal);
        jugador.liberarTokens(cantidad);

        tenenciaTokenRepository.save(TenenciaTokenEntity.desde(tenencia));
        usuarioRepository.save(UsuarioEntity.desde(usuario));
        jugadorRepository.save(JugadorEntity.desde(jugador));

        return registrarMovimiento(
                usuarioId,
                jugadorId,
                TipoMovimiento.VENTA_SISTEMA,
                cantidad,
                precioVenta,
                montoTotal);
    }

    private Movimiento registrarMovimiento(
            UUID usuarioId,
            UUID jugadorId,
            TipoMovimiento tipo,
            int cantidad,
            BigDecimal precioUnitario,
            BigDecimal montoTotal) {
        return movimientoRepository
                .save(
                        MovimientoEntity.desde(
                                Movimiento.registrar(
                                        usuarioId,
                                        jugadorId,
                                        tipo,
                                        cantidad,
                                        precioUnitario,
                                        montoTotal,
                                        null)))
                .aModelo();
    }

    private Usuario obtenerUsuario(UUID usuarioId) {
        return usuarioRepository
                .findById(usuarioId)
                .map(UsuarioEntity::aModelo)
                .orElseThrow(() -> new RecursoNoEncontradoException(ENTIDAD_USUARIO, usuarioId));
    }

    private Jugador obtenerJugador(UUID jugadorId) {
        return jugadorRepository
                .findById(jugadorId)
                .map(JugadorEntity::aModelo)
                .orElseThrow(() -> new RecursoNoEncontradoException(ENTIDAD_JUGADOR, jugadorId));
    }

    private TenenciaToken obtenerOAbrirTenencia(UUID usuarioId, UUID jugadorId) {
        return tenenciaTokenRepository
                .findByUsuarioIdAndJugadorId(usuarioId, jugadorId)
                .map(TenenciaTokenEntity::aModelo)
                .orElseGet(() -> TenenciaToken.abrir(usuarioId, jugadorId));
    }

    private BigDecimal cotizacionVigenteDe(UUID jugadorId) {
        return cotizacionHistoricaRepository
                .findTopByJugadorIdOrderByFechaCalculoDesc(jugadorId)
                .map(CotizacionHistoricaEntity::getValor)
                .orElse(valorInicial);
    }

    private static BigDecimal costoDe(int cantidad, BigDecimal cotizacionVigente) {
        return Monetario.escalar(cotizacionVigente.multiply(BigDecimal.valueOf(cantidad)));
    }

    /**
     * Precio unitario que paga el sistema al comprarle tokens a un usuario (UC-10): {@code
     * app.token.descuento-venta-sistema} por debajo de la cotización vigente — nunca la cotización
     * completa, a diferencia de {@link #comprar} y de toda operación P2P ({@code
     * OfertaServiceImpl}, UC-17). Redondeado antes de multiplicarlo por la cantidad en {@link
     * #costoDe}, igual criterio que el resto de los valores monetarios del sistema (constitution.md
     * §2, {@link Monetario}).
     */
    private BigDecimal precioVentaSistemaDe(BigDecimal cotizacionVigente) {
        return Monetario.escalar(
                cotizacionVigente.multiply(BigDecimal.ONE.subtract(descuentoVentaSistema)));
    }
}
