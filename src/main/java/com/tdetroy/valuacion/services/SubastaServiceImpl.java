package com.tdetroy.valuacion.services;

import com.tdetroy.valuacion.common.exceptions.OperacionSubastaInvalidaException;
import com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException;
import com.tdetroy.valuacion.entity.JugadorEntity;
import com.tdetroy.valuacion.entity.MovimientoEntity;
import com.tdetroy.valuacion.entity.PujaSubastaEntity;
import com.tdetroy.valuacion.entity.SubastaEntity;
import com.tdetroy.valuacion.entity.TenenciaTokenEntity;
import com.tdetroy.valuacion.entity.UsuarioEntity;
import com.tdetroy.valuacion.model.EstadoJugador;
import com.tdetroy.valuacion.model.EstadoSubasta;
import com.tdetroy.valuacion.model.IncrementoPuja;
import com.tdetroy.valuacion.model.Jugador;
import com.tdetroy.valuacion.model.Movimiento;
import com.tdetroy.valuacion.model.PujaSubasta;
import com.tdetroy.valuacion.model.Subasta;
import com.tdetroy.valuacion.model.TenenciaToken;
import com.tdetroy.valuacion.model.TipoMovimiento;
import com.tdetroy.valuacion.model.Usuario;
import com.tdetroy.valuacion.repositories.JugadorRepository;
import com.tdetroy.valuacion.repositories.MovimientoRepository;
import com.tdetroy.valuacion.repositories.PujaSubastaRepository;
import com.tdetroy.valuacion.repositories.SubastaRepository;
import com.tdetroy.valuacion.repositories.TenenciaTokenRepository;
import com.tdetroy.valuacion.repositories.UsuarioRepository;
import java.math.BigDecimal;
import java.time.Duration;
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
 * <p>Toda mutación relee la subasta vía {@link SubastaRepository#findById} ({@code
 * PESSIMISTIC_WRITE}): dos pujas simultáneas, o una puja y el cierre automático, nunca se pisan.
 * {@code self} (proxy de Spring, inyectado {@code @Lazy}) permite que {@link #cerrarUna} abra su
 * propia transacción por subasta, mismo criterio que {@code OfertaServiceImpl#cancelarUnaVencida}.
 */
@Service
public class SubastaServiceImpl implements SubastaService {

    private static final Logger log = LoggerFactory.getLogger(SubastaServiceImpl.class);

    private static final String ENTIDAD_JUGADOR = "Jugador";
    private static final String ENTIDAD_USUARIO = "Usuario";
    private static final String ENTIDAD_SUBASTA = "Subasta";
    private static final String ACCION_ADJUDICACION = "ADJUDICACION_SUBASTA";

    private final SubastaRepository subastaRepository;
    private final PujaSubastaRepository pujaSubastaRepository;
    private final TenenciaTokenRepository tenenciaTokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final JugadorRepository jugadorRepository;
    private final MovimientoRepository movimientoRepository;
    private final AuditoriaService auditoriaService;
    private final SubastaService self;

    public SubastaServiceImpl(
            SubastaRepository subastaRepository,
            PujaSubastaRepository pujaSubastaRepository,
            TenenciaTokenRepository tenenciaTokenRepository,
            UsuarioRepository usuarioRepository,
            JugadorRepository jugadorRepository,
            MovimientoRepository movimientoRepository,
            AuditoriaService auditoriaService,
            @Lazy SubastaService self) {
        this.subastaRepository = subastaRepository;
        this.pujaSubastaRepository = pujaSubastaRepository;
        this.tenenciaTokenRepository = tenenciaTokenRepository;
        this.usuarioRepository = usuarioRepository;
        this.jugadorRepository = jugadorRepository;
        this.movimientoRepository = movimientoRepository;
        this.auditoriaService = auditoriaService;
        this.self = self;
    }

    @Override
    @Transactional
    public Subasta crear(
            UUID vendedorId,
            UUID jugadorId,
            int cantidad,
            BigDecimal precioInicial,
            long duracionMinutos) {
        Jugador jugador =
                jugadorRepository
                        .findById(jugadorId)
                        .map(JugadorEntity::aModelo)
                        .orElseThrow(
                                () -> new RecursoNoEncontradoException(ENTIDAD_JUGADOR, jugadorId));
        if (jugador.getEstado() != EstadoJugador.ACTIVO) {
            throw new OperacionSubastaInvalidaException(
                    "No se puede subastar tokens de un jugador dado de baja");
        }

        TenenciaToken tenencia = obtenerOAbrirTenencia(vendedorId, jugadorId);
        tenencia.reservar(cantidad);
        tenenciaTokenRepository.save(TenenciaTokenEntity.desde(tenencia));

        Subasta subasta =
                Subasta.crear(
                        vendedorId,
                        jugadorId,
                        cantidad,
                        precioInicial,
                        Duration.ofMinutes(duracionMinutos),
                        Instant.now());
        subastaRepository.save(SubastaEntity.desde(subasta));
        return subasta;
    }

    @Override
    public List<Subasta> listarActivas() {
        return subastaRepository.findByEstadoOrderByFechaCreacionDesc(EstadoSubasta.ACTIVA).stream()
                .map(SubastaEntity::aModelo)
                .toList();
    }

    @Override
    public List<Subasta> listarActivasDe(UUID jugadorId) {
        return subastaRepository
                .findByJugadorIdAndEstadoOrderByFechaCreacionDesc(jugadorId, EstadoSubasta.ACTIVA)
                .stream()
                .map(SubastaEntity::aModelo)
                .toList();
    }

    @Override
    public SubastaDetalle obtenerDetalle(UUID subastaId) {
        Subasta subasta =
                subastaRepository
                        .findOneById(subastaId)
                        .map(SubastaEntity::aModelo)
                        .orElseThrow(
                                () -> new RecursoNoEncontradoException(ENTIDAD_SUBASTA, subastaId));
        List<PujaSubasta> pujas =
                pujaSubastaRepository.findBySubastaIdOrderByFechaDesc(subastaId).stream()
                        .map(PujaSubastaEntity::aModelo)
                        .toList();
        return new SubastaDetalle(subasta, pujas);
    }

    @Override
    @Transactional
    public Subasta pujar(UUID postorId, UUID subastaId, int porcentaje) {
        IncrementoPuja incremento = IncrementoPuja.desdePorcentaje(porcentaje);
        Subasta subasta = obtenerBajoLock(subastaId);
        UUID postorAnteriorId = subasta.getMejorPostorId();
        BigDecimal montoAnterior = subasta.montoComprometido();

        Instant ahora = Instant.now();
        BigDecimal nuevoPrecio = subasta.pujar(postorId, incremento, ahora);
        BigDecimal nuevoMonto = subasta.montoTotal(nuevoPrecio);

        Usuario postor = obtenerUsuario(postorId);
        postor.debitarSaldo(nuevoMonto);
        usuarioRepository.save(UsuarioEntity.desde(postor));

        if (postorAnteriorId != null) {
            devolverSaldo(postorAnteriorId, montoAnterior);
        }

        subastaRepository.save(SubastaEntity.desde(subasta));
        pujaSubastaRepository.save(
                PujaSubastaEntity.desde(
                        PujaSubasta.registrar(subastaId, postorId, nuevoPrecio, ahora)));
        return subasta;
    }

    @Override
    @Transactional
    public void cancelar(UUID vendedorId, UUID subastaId) {
        Subasta subasta = obtenerBajoLock(subastaId);
        if (!subasta.getVendedorId().equals(vendedorId)) {
            throw new OperacionSubastaInvalidaException(
                    "Sólo el vendedor puede cancelar la subasta %s".formatted(subastaId));
        }
        cancelarYDevolver(subasta);
    }

    @Override
    @Transactional
    public void cancelarActivasDe(UUID jugadorId) {
        for (SubastaEntity entity :
                subastaRepository.findByJugadorIdAndEstadoOrderByFechaCreacionDesc(
                        jugadorId, EstadoSubasta.ACTIVA)) {
            Subasta subasta = obtenerBajoLock(entity.getId());
            if (subasta.getEstado() == EstadoSubasta.ACTIVA) {
                cancelarYDevolver(subasta);
            }
        }
    }

    @Override
    public void cerrarVencidas() {
        Instant ahora = Instant.now();
        for (SubastaEntity entity :
                subastaRepository.findByEstadoOrderByFechaCreacionDesc(EstadoSubasta.ACTIVA)) {
            Subasta subasta = entity.aModelo();
            if (!subasta.debeAdjudicarse(ahora) && !subasta.quedoDesierta(ahora)) {
                continue;
            }
            try {
                self.cerrarUna(entity.getId());
            } catch (RuntimeException ex) {
                log.error(
                        "Fallo al cerrar automáticamente la subasta {}: {}",
                        entity.getId(),
                        ex.getMessage(),
                        ex);
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cerrarUna(UUID subastaId) {
        // Releída bajo lock: puede haber entrado una puja (que reinicia el plazo) o una
        // cancelación entre la lectura sin lock de cerrarVencidas() y este momento.
        Subasta subasta = obtenerBajoLock(subastaId);
        Instant ahora = Instant.now();
        if (subasta.debeAdjudicarse(ahora)) {
            adjudicar(subasta, ahora);
        } else if (subasta.quedoDesierta(ahora)) {
            subasta.declararDesierta(ahora);
            liberarTokens(subasta);
            subastaRepository.save(SubastaEntity.desde(subasta));
        }
    }

    /**
     * Tokens reservados del vendedor → ganador; saldo ya reservado del ganador → vendedor. El
     * ganador no se debita acá: se le descontó al pujar.
     */
    private void adjudicar(Subasta subasta, Instant ahora) {
        subasta.adjudicar(ahora);
        UUID vendedorId = subasta.getVendedorId();
        UUID ganadorId = subasta.getMejorPostorId();
        int cantidad = subasta.getCantidad();
        BigDecimal precio = subasta.getPrecioActual();
        BigDecimal monto = subasta.montoComprometido();

        TenenciaToken tenenciaVendedor = obtenerOAbrirTenencia(vendedorId, subasta.getJugadorId());
        tenenciaVendedor.ejecutarReserva(cantidad);
        TenenciaToken tenenciaGanador = obtenerOAbrirTenencia(ganadorId, subasta.getJugadorId());
        tenenciaGanador.acreditar(cantidad);
        tenenciaTokenRepository.save(TenenciaTokenEntity.desde(tenenciaVendedor));
        tenenciaTokenRepository.save(TenenciaTokenEntity.desde(tenenciaGanador));

        Usuario vendedor = obtenerUsuario(vendedorId);
        vendedor.acreditarSaldo(monto);
        usuarioRepository.save(UsuarioEntity.desde(vendedor));

        subastaRepository.save(SubastaEntity.desde(subasta));

        registrarMovimiento(
                vendedorId,
                subasta.getJugadorId(),
                TipoMovimiento.VENTA_SUBASTA,
                cantidad,
                precio,
                monto,
                ganadorId);
        registrarMovimiento(
                ganadorId,
                subasta.getJugadorId(),
                TipoMovimiento.COMPRA_SUBASTA,
                cantidad,
                precio,
                monto,
                vendedorId);

        auditoriaService.registrar(
                null,
                ACCION_ADJUDICACION,
                ENTIDAD_SUBASTA,
                subasta.getId(),
                null,
                new AdjudicacionAuditoria(vendedorId, ganadorId, cantidad, precio, monto));
    }

    private void cancelarYDevolver(Subasta subasta) {
        UUID mejorPostorId = subasta.getMejorPostorId();
        BigDecimal montoComprometido = subasta.montoComprometido();
        subasta.cancelar();
        liberarTokens(subasta);
        if (mejorPostorId != null) {
            devolverSaldo(mejorPostorId, montoComprometido);
        }
        subastaRepository.save(SubastaEntity.desde(subasta));
    }

    private void liberarTokens(Subasta subasta) {
        TenenciaToken tenencia =
                obtenerOAbrirTenencia(subasta.getVendedorId(), subasta.getJugadorId());
        tenencia.liberarReserva(subasta.getCantidad());
        tenenciaTokenRepository.save(TenenciaTokenEntity.desde(tenencia));
    }

    private void devolverSaldo(UUID usuarioId, BigDecimal monto) {
        Usuario usuario = obtenerUsuario(usuarioId);
        usuario.acreditarSaldo(monto);
        usuarioRepository.save(UsuarioEntity.desde(usuario));
    }

    private Subasta obtenerBajoLock(UUID subastaId) {
        return subastaRepository
                .findById(subastaId)
                .map(SubastaEntity::aModelo)
                .orElseThrow(() -> new RecursoNoEncontradoException(ENTIDAD_SUBASTA, subastaId));
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

    private void registrarMovimiento(
            UUID usuarioId,
            UUID jugadorId,
            TipoMovimiento tipo,
            int cantidad,
            BigDecimal precioUnitario,
            BigDecimal montoTotal,
            UUID contraparteUsuarioId) {
        movimientoRepository.save(
                MovimientoEntity.desde(
                        Movimiento.registrar(
                                usuarioId,
                                jugadorId,
                                tipo,
                                cantidad,
                                precioUnitario,
                                montoTotal,
                                contraparteUsuarioId)));
    }

    private record AdjudicacionAuditoria(
            UUID vendedorId,
            UUID ganadorId,
            int cantidad,
            BigDecimal precioUnitario,
            BigDecimal montoTotal) {}
}
