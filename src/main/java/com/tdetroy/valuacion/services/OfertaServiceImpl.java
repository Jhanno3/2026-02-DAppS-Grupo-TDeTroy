package com.tdetroy.valuacion.services;

import com.tdetroy.valuacion.common.Monetario;
import com.tdetroy.valuacion.common.exceptions.OfertaInsuficienteException;
import com.tdetroy.valuacion.common.exceptions.OfertaNoAbiertaException;
import com.tdetroy.valuacion.common.exceptions.OfertaNoPerteneceAlVendedorException;
import com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException;
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
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@inheritDoc}
 *
 * <p><b>self (igual criterio que {@code CotizacionServiceImpl}, tasks.md T4.5):</b> {@link
 * #cancelarVencidas} nunca llama a {@code this.cancelarUnaVencida(...)} directamente — una
 * auto-invocación así saltea el proxy AOP de Spring y el {@code @Transactional(REQUIRES_NEW)} de
 * {@link #cancelarUnaVencida} quedaría sin efecto. Se inyecta la propia interfaz vía {@code @Lazy}
 * (constructor, no atributo) para forzar el paso por el proxy real.
 */
@Service
public class OfertaServiceImpl implements OfertaService {

    private static final Logger log = LoggerFactory.getLogger(OfertaServiceImpl.class);

    private static final String ENTIDAD_JUGADOR = "Jugador";
    private static final String ENTIDAD_USUARIO = "Usuario";
    private static final String ENTIDAD_OFERTA = "OfertaP2P";
    private static final String ACCION_EJECUCION = "EJECUCION_OFERTA_P2P";

    private final OfertaP2PRepository ofertaP2PRepository;
    private final TenenciaTokenRepository tenenciaTokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final JugadorRepository jugadorRepository;
    private final MovimientoRepository movimientoRepository;
    private final AuditoriaService auditoriaService;
    private final OfertaService self;

    public OfertaServiceImpl(
            OfertaP2PRepository ofertaP2PRepository,
            TenenciaTokenRepository tenenciaTokenRepository,
            UsuarioRepository usuarioRepository,
            JugadorRepository jugadorRepository,
            MovimientoRepository movimientoRepository,
            AuditoriaService auditoriaService,
            @Lazy OfertaService self) {
        this.ofertaP2PRepository = ofertaP2PRepository;
        this.tenenciaTokenRepository = tenenciaTokenRepository;
        this.usuarioRepository = usuarioRepository;
        this.jugadorRepository = jugadorRepository;
        this.movimientoRepository = movimientoRepository;
        this.auditoriaService = auditoriaService;
        this.self = self;
    }

    @Override
    @Transactional
    public OfertaP2P publicar(
            UUID vendedorId,
            UUID jugadorId,
            int cantidad,
            BigDecimal precioUnitario,
            long horasLimite) {
        if (!jugadorRepository.existsById(jugadorId)) {
            throw new RecursoNoEncontradoException(ENTIDAD_JUGADOR, jugadorId);
        }

        TenenciaToken tenencia = obtenerOAbrirTenencia(vendedorId, jugadorId);
        tenencia.reservar(cantidad);
        tenenciaTokenRepository.save(TenenciaTokenEntity.desde(tenencia));

        OfertaP2P oferta =
                OfertaP2P.publicar(vendedorId, jugadorId, cantidad, precioUnitario, horasLimite);
        ofertaP2PRepository.save(OfertaP2PEntity.desde(oferta));
        return oferta;
    }

    @Override
    public List<OfertaP2P> listarAbiertasDe(UUID jugadorId) {
        return ofertaP2PRepository
                .findByJugadorIdAndEstado(jugadorId, EstadoOferta.ABIERTA)
                .stream()
                .map(OfertaP2PEntity::aModelo)
                .toList();
    }

    @Override
    public List<OfertaP2P> listarAbiertas() {
        return ofertaP2PRepository.findByEstado(EstadoOferta.ABIERTA).stream()
                .map(OfertaP2PEntity::aModelo)
                .toList();
    }

    @Override
    @Transactional
    public Movimiento ejecutar(UUID compradorId, UUID ofertaId, int cantidad) {
        Usuario comprador = obtenerUsuario(compradorId);
        OfertaP2P oferta = obtenerOfertaBajoLock(ofertaId);

        if (cantidad > oferta.getCantidadDisponible()) {
            throw new OfertaInsuficienteException(cantidad, oferta.getCantidadDisponible());
        }

        BigDecimal precioUnitario = oferta.getPrecioUnitario();
        BigDecimal montoTotal = costoDe(cantidad, precioUnitario);

        comprador.debitarSaldo(montoTotal);
        UUID vendedorId = oferta.getVendedorId();
        Usuario vendedor = obtenerUsuario(vendedorId);
        vendedor.acreditarSaldo(montoTotal);

        TenenciaToken tenenciaVendedor = obtenerOAbrirTenencia(vendedorId, oferta.getJugadorId());
        tenenciaVendedor.ejecutarReserva(cantidad);
        TenenciaToken tenenciaComprador = obtenerOAbrirTenencia(compradorId, oferta.getJugadorId());
        tenenciaComprador.acreditar(cantidad);

        oferta.ejecutarContra(cantidad);

        usuarioRepository.save(UsuarioEntity.desde(comprador));
        usuarioRepository.save(UsuarioEntity.desde(vendedor));
        tenenciaTokenRepository.save(TenenciaTokenEntity.desde(tenenciaVendedor));
        tenenciaTokenRepository.save(TenenciaTokenEntity.desde(tenenciaComprador));
        ofertaP2PRepository.save(OfertaP2PEntity.desde(oferta));

        registrarMovimiento(
                vendedorId,
                oferta.getJugadorId(),
                TipoMovimiento.VENTA_P2P,
                cantidad,
                precioUnitario,
                montoTotal,
                compradorId);
        Movimiento movimientoComprador =
                registrarMovimiento(
                        compradorId,
                        oferta.getJugadorId(),
                        TipoMovimiento.COMPRA_P2P,
                        cantidad,
                        precioUnitario,
                        montoTotal,
                        vendedorId);

        auditoriaService.registrar(
                compradorId,
                ACCION_EJECUCION,
                ENTIDAD_OFERTA,
                ofertaId,
                null,
                new EjecucionOfertaAuditoria(vendedorId, compradorId, cantidad, montoTotal));

        return movimientoComprador;
    }

    @Override
    @Transactional
    public void cancelar(UUID vendedorId, UUID ofertaId) {
        OfertaP2P oferta = obtenerOfertaBajoLock(ofertaId);

        if (!oferta.getVendedorId().equals(vendedorId)) {
            throw new OfertaNoPerteneceAlVendedorException(ofertaId, vendedorId);
        }
        if (oferta.getEstado() != EstadoOferta.ABIERTA) {
            throw new OfertaNoAbiertaException(ofertaId, oferta.getEstado());
        }

        liberarReservaYCancelar(oferta);
    }

    @Override
    public void cancelarVencidas() {
        Instant ahora = Instant.now();
        for (OfertaP2PEntity ofertaEntity :
                ofertaP2PRepository.findByEstadoAndFechaVencimientoBefore(
                        EstadoOferta.ABIERTA, ahora)) {
            try {
                // Vía `self` (proxy de Spring): asegura que @Transactional(REQUIRES_NEW) de
                // cancelarUnaVencida efectivamente abra una sub-transacción propia por oferta
                // (mismo criterio que CotizacionServiceImpl.recalcular) -- una falla puntual en
                // una oferta no debe impedir cancelar las demás ya vencidas de esta misma pasada.
                self.cancelarUnaVencida(ofertaEntity.getId());
            } catch (RuntimeException ex) {
                log.error(
                        "Fallo al cancelar automáticamente la oferta P2P vencida {}: {}",
                        ofertaEntity.getId(),
                        ex.getMessage(),
                        ex);
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelarUnaVencida(UUID ofertaId) {
        OfertaP2P oferta = obtenerOfertaBajoLock(ofertaId);
        // Releída bajo lock: puede haberse vendido/cancelado entre el findByEstadoAndFecha...
        // (sin lock) de cancelarVencidas() y este momento -- sólo actúa si sigue ABIERTA.
        if (oferta.getEstado() == EstadoOferta.ABIERTA) {
            liberarReservaYCancelar(oferta);
        }
    }

    private void liberarReservaYCancelar(OfertaP2P oferta) {
        TenenciaToken tenencia =
                obtenerOAbrirTenencia(oferta.getVendedorId(), oferta.getJugadorId());
        tenencia.liberarReserva(oferta.getCantidadDisponible());
        tenenciaTokenRepository.save(TenenciaTokenEntity.desde(tenencia));

        oferta.cancelar();
        ofertaP2PRepository.save(OfertaP2PEntity.desde(oferta));
    }

    private OfertaP2P obtenerOfertaBajoLock(UUID ofertaId) {
        return ofertaP2PRepository
                .findById(ofertaId)
                .map(OfertaP2PEntity::aModelo)
                .orElseThrow(() -> new RecursoNoEncontradoException(ENTIDAD_OFERTA, ofertaId));
    }

    private Usuario obtenerUsuario(UUID usuarioId) {
        return usuarioRepository
                .findById(usuarioId)
                .map(UsuarioEntity::aModelo)
                .orElseThrow(() -> new RecursoNoEncontradoException(ENTIDAD_USUARIO, usuarioId));
    }

    private TenenciaToken obtenerOAbrirTenencia(UUID usuarioId, UUID jugadorId) {
        return tenenciaTokenRepository
                .findByUsuarioIdAndJugadorId(usuarioId, jugadorId)
                .map(TenenciaTokenEntity::aModelo)
                .orElseGet(() -> TenenciaToken.abrir(usuarioId, jugadorId));
    }

    private static BigDecimal costoDe(int cantidad, BigDecimal precioUnitario) {
        return Monetario.escalar(precioUnitario.multiply(BigDecimal.valueOf(cantidad)));
    }

    private Movimiento registrarMovimiento(
            UUID usuarioId,
            UUID jugadorId,
            TipoMovimiento tipo,
            int cantidad,
            BigDecimal precioUnitario,
            BigDecimal montoTotal,
            UUID contraparteUsuarioId) {
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
                                        contraparteUsuarioId)))
                .aModelo();
    }

    private record EjecucionOfertaAuditoria(
            UUID vendedorId, UUID compradorId, int cantidad, BigDecimal montoTotal) {}
}
