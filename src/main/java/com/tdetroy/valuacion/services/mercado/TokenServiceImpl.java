package com.tdetroy.valuacion.services.mercado;

import com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException;
import com.tdetroy.valuacion.entity.cartera.MovimientoEntity;
import com.tdetroy.valuacion.entity.cartera.TenenciaTokenEntity;
import com.tdetroy.valuacion.entity.cotizacion.CotizacionHistoricaEntity;
import com.tdetroy.valuacion.entity.jugador.JugadorEntity;
import com.tdetroy.valuacion.entity.usuario.UsuarioEntity;
import com.tdetroy.valuacion.model.cartera.Movimiento;
import com.tdetroy.valuacion.model.cartera.TenenciaToken;
import com.tdetroy.valuacion.model.jugador.Jugador;
import com.tdetroy.valuacion.model.usuario.Usuario;
import com.tdetroy.valuacion.repositories.cartera.MovimientoRepository;
import com.tdetroy.valuacion.repositories.cartera.TenenciaTokenRepository;
import com.tdetroy.valuacion.repositories.cotizacion.CotizacionHistoricaRepository;
import com.tdetroy.valuacion.repositories.jugador.JugadorRepository;
import com.tdetroy.valuacion.repositories.usuario.UsuarioRepository;
import com.tdetroy.valuacion.services.InvalidaRanking;
import com.tdetroy.valuacion.services.auditoria.AuditoriaService;
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
    private final AuditoriaService auditoriaService;
    private final BigDecimal valorInicial;
    private final BigDecimal descuentoVentaSistema;

    public TokenServiceImpl(
            UsuarioRepository usuarioRepository,
            JugadorRepository jugadorRepository,
            TenenciaTokenRepository tenenciaTokenRepository,
            CotizacionHistoricaRepository cotizacionHistoricaRepository,
            MovimientoRepository movimientoRepository,
            AuditoriaService auditoriaService,
            @Value("${app.cotizacion.valor-inicial}") BigDecimal valorInicial,
            @Value("${app.token.descuento-venta-sistema}") BigDecimal descuentoVentaSistema) {
        this.usuarioRepository = usuarioRepository;
        this.jugadorRepository = jugadorRepository;
        this.tenenciaTokenRepository = tenenciaTokenRepository;
        this.cotizacionHistoricaRepository = cotizacionHistoricaRepository;
        this.movimientoRepository = movimientoRepository;
        this.auditoriaService = auditoriaService;
        this.valorInicial = valorInicial;
        this.descuentoVentaSistema = descuentoVentaSistema;
    }

    @Override
    @InvalidaRanking
    @Transactional
    public Movimiento comprar(UUID usuarioId, UUID jugadorId, int cantidad) {
        Usuario usuario = obtenerUsuario(usuarioId);
        Jugador jugador = obtenerJugador(jugadorId);
        Movimiento compra =
                Movimiento.compraSistema(
                        usuarioId, jugadorId, cantidad, cotizacionVigenteDe(jugadorId));

        BigDecimal saldoAntes = usuario.getSaldoVirtual();
        int tokensEmitidosAntes = jugador.getTokensEmitidos();

        usuario.debitarSaldo(compra.getMontoTotal());
        jugador.emitirTokens(cantidad);
        TenenciaToken tenencia = obtenerOAbrirTenencia(usuarioId, jugadorId);
        var antes =
                new TokenOperacionAuditoria(
                        jugadorId,
                        cantidad,
                        saldoAntes,
                        tokensEmitidosAntes,
                        tenencia.getCantidad());
        tenencia.acreditar(cantidad, compra.getPrecioUnitario());

        usuarioRepository.save(UsuarioEntity.desde(usuario));
        jugadorRepository.save(JugadorEntity.desde(jugador));
        tenenciaTokenRepository.save(TenenciaTokenEntity.desde(tenencia));

        Movimiento guardada = guardar(compra);
        registrarAuditoria(
                "COMPRA_TOKEN", usuarioId, jugadorId, cantidad, antes, usuario, jugador, tenencia);
        return guardada;
    }

    @Override
    @InvalidaRanking
    @Transactional
    public Movimiento vender(UUID usuarioId, UUID jugadorId, int cantidad) {
        Usuario usuario = obtenerUsuario(usuarioId);
        Jugador jugador = obtenerJugador(jugadorId);
        TenenciaToken tenencia = obtenerOAbrirTenencia(usuarioId, jugadorId);

        tenencia.validarDebito(cantidad);
        var antes =
                new TokenOperacionAuditoria(
                        jugadorId,
                        cantidad,
                        usuario.getSaldoVirtual(),
                        jugador.getTokensEmitidos(),
                        tenencia.getCantidad());

        // El descuento de venta al sistema lo aplica Movimiento#ventaSistema (UC-10).
        Movimiento venta =
                Movimiento.ventaSistema(
                        usuarioId,
                        jugadorId,
                        cantidad,
                        cotizacionVigenteDe(jugadorId),
                        descuentoVentaSistema);
        tenencia.debitar(cantidad, venta.getMontoTotal());
        usuario.acreditarSaldo(venta.getMontoTotal());
        jugador.liberarTokens(cantidad);

        tenenciaTokenRepository.save(TenenciaTokenEntity.desde(tenencia));
        usuarioRepository.save(UsuarioEntity.desde(usuario));
        jugadorRepository.save(JugadorEntity.desde(jugador));

        Movimiento guardada = guardar(venta);
        registrarAuditoria(
                "VENTA_TOKEN", usuarioId, jugadorId, cantidad, antes, usuario, jugador, tenencia);
        return guardada;
    }

    private void registrarAuditoria(
            String accion,
            UUID usuarioId,
            UUID jugadorId,
            int cantidad,
            TokenOperacionAuditoria antes,
            Usuario usuario,
            Jugador jugador,
            TenenciaToken tenencia) {
        var despues =
                new TokenOperacionAuditoria(
                        jugadorId,
                        cantidad,
                        usuario.getSaldoVirtual(),
                        jugador.getTokensEmitidos(),
                        tenencia.getCantidad());
        auditoriaService.registrar(usuarioId, accion, ENTIDAD_USUARIO, usuarioId, antes, despues);
    }

    private record TokenOperacionAuditoria(
            UUID jugadorId,
            int cantidad,
            BigDecimal saldoVirtual,
            int tokensEmitidos,
            int cantidadTenencia) {}

    private Movimiento guardar(Movimiento movimiento) {
        return movimientoRepository.save(MovimientoEntity.desde(movimiento)).aModelo();
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
}
