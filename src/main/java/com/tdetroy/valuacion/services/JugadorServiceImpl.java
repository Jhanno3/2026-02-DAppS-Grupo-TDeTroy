package com.tdetroy.valuacion.services;

import com.tdetroy.valuacion.common.Monetario;
import com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException;
import com.tdetroy.valuacion.entity.CotizacionHistoricaEntity;
import com.tdetroy.valuacion.entity.JugadorEntity;
import com.tdetroy.valuacion.entity.MovimientoEntity;
import com.tdetroy.valuacion.entity.OfertaP2PEntity;
import com.tdetroy.valuacion.entity.TenenciaTokenEntity;
import com.tdetroy.valuacion.entity.UsuarioEntity;
import com.tdetroy.valuacion.model.EstadoOferta;
import com.tdetroy.valuacion.model.Jugador;
import com.tdetroy.valuacion.model.Movimiento;
import com.tdetroy.valuacion.model.OfertaP2P;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** {@inheritDoc} */
@Service
public class JugadorServiceImpl implements JugadorService {

    private static final String ENTIDAD_AUDITADA = "Jugador";
    private static final String ENTIDAD_USUARIO = "Usuario";
    private static final String ACCION_BAJA = "BAJA_JUGADOR";

    private final JugadorRepository jugadorRepository;
    private final OfertaP2PRepository ofertaP2PRepository;
    private final TenenciaTokenRepository tenenciaTokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final CotizacionHistoricaRepository cotizacionHistoricaRepository;
    private final MovimientoRepository movimientoRepository;
    private final AuditoriaService auditoriaService;
    private final BigDecimal valorInicial;

    public JugadorServiceImpl(
            JugadorRepository jugadorRepository,
            OfertaP2PRepository ofertaP2PRepository,
            TenenciaTokenRepository tenenciaTokenRepository,
            UsuarioRepository usuarioRepository,
            CotizacionHistoricaRepository cotizacionHistoricaRepository,
            MovimientoRepository movimientoRepository,
            AuditoriaService auditoriaService,
            @Value("${app.cotizacion.valor-inicial}") BigDecimal valorInicial) {
        this.jugadorRepository = jugadorRepository;
        this.ofertaP2PRepository = ofertaP2PRepository;
        this.tenenciaTokenRepository = tenenciaTokenRepository;
        this.usuarioRepository = usuarioRepository;
        this.cotizacionHistoricaRepository = cotizacionHistoricaRepository;
        this.movimientoRepository = movimientoRepository;
        this.auditoriaService = auditoriaService;
        this.valorInicial = valorInicial;
    }

    @Override
    public List<Jugador> listar() {
        return jugadorRepository.findAll().stream().map(JugadorEntity::aModelo).toList();
    }

    @Override
    public Jugador obtenerPorId(UUID jugadorId) {
        return jugadorRepository
                .findById(jugadorId)
                .map(JugadorEntity::aModelo)
                .orElseThrow(() -> new RecursoNoEncontradoException(ENTIDAD_AUDITADA, jugadorId));
    }

    @Override
    @Transactional
    public Jugador darAlta(
            String nombre,
            String club,
            String posicion,
            LocalDate fechaNacimiento,
            String nacionalidad,
            String liga,
            UUID actorId) {
        Jugador jugador =
                jugadorRepository
                        .save(
                                JugadorEntity.desde(
                                        Jugador.darAlta(
                                                nombre,
                                                club,
                                                posicion,
                                                fechaNacimiento,
                                                nacionalidad,
                                                liga)))
                        .aModelo();

        auditoriaService.registrar(
                actorId,
                "ALTA_JUGADOR",
                ENTIDAD_AUDITADA,
                jugador.getId(),
                null,
                datosIdentificatoriosDe(jugador));

        return jugador;
    }

    @Override
    @Transactional
    public Jugador editar(
            UUID jugadorId,
            String nombre,
            String club,
            String posicion,
            LocalDate fechaNacimiento,
            String nacionalidad,
            String liga,
            UUID actorId) {
        Jugador jugador =
                jugadorRepository
                        .findById(jugadorId)
                        .map(JugadorEntity::aModelo)
                        .orElseThrow(
                                () ->
                                        new RecursoNoEncontradoException(
                                                ENTIDAD_AUDITADA, jugadorId));

        DatosIdentificatoriosAuditoria antes = datosIdentificatoriosDe(jugador);
        jugador.actualizarDatosIdentificatorios(
                nombre, club, posicion, fechaNacimiento, nacionalidad, liga);
        jugadorRepository.save(JugadorEntity.desde(jugador));

        auditoriaService.registrar(
                actorId,
                "EDICION_JUGADOR",
                ENTIDAD_AUDITADA,
                jugadorId,
                antes,
                datosIdentificatoriosDe(jugador));

        return jugador;
    }

    @Override
    @Transactional
    public Jugador darDeBaja(UUID jugadorId, UUID actorId) {
        Jugador jugador =
                jugadorRepository
                        .findById(jugadorId)
                        .map(JugadorEntity::aModelo)
                        .orElseThrow(
                                () ->
                                        new RecursoNoEncontradoException(
                                                ENTIDAD_AUDITADA, jugadorId));

        cancelarOfertasAbiertasDe(jugadorId);
        BigDecimal cotizacionReferencia = cotizacionVigenteDe(jugadorId);
        List<CompensacionBaja> compensaciones =
                compensarTenenciasDe(jugadorId, cotizacionReferencia);

        jugador.darDeBaja();
        jugadorRepository.save(JugadorEntity.desde(jugador));

        auditoriaService.registrar(
                actorId,
                ACCION_BAJA,
                ENTIDAD_AUDITADA,
                jugadorId,
                null,
                new BajaJugadorAuditoria(cotizacionReferencia, compensaciones));

        return jugador;
    }

    /**
     * Paso 1 de la baja (plan.md §5/§9.5): cada oferta {@code ABIERTA} de este jugador se cancela y
     * la cantidad que tenía reservada se reintegra a la tenencia de su vendedor — antes de
     * compensar, para que esa tenencia quede completa al valorizarla.
     */
    private void cancelarOfertasAbiertasDe(UUID jugadorId) {
        for (OfertaP2PEntity ofertaEntity :
                ofertaP2PRepository.findByJugadorIdAndEstado(jugadorId, EstadoOferta.ABIERTA)) {
            // Re-lee bajo @Lock(PESSIMISTIC_WRITE) (igual que OfertaServiceImpl.ejecutar/cancelar,
            // plan.md §9.3): evita pisar una compra concurrente que esté ejecutándose sobre esta
            // misma oferta en este preciso momento.
            OfertaP2P oferta =
                    ofertaP2PRepository.findById(ofertaEntity.getId()).orElseThrow().aModelo();

            TenenciaToken tenenciaVendedor =
                    obtenerOAbrirTenencia(oferta.getVendedorId(), jugadorId);
            tenenciaVendedor.liberarReserva(oferta.getCantidadDisponible());
            tenenciaTokenRepository.save(TenenciaTokenEntity.desde(tenenciaVendedor));

            oferta.cancelar();
            ofertaP2PRepository.save(OfertaP2PEntity.desde(oferta));
        }
    }

    /**
     * Paso 2 de la baja (plan.md §5, UC-15): cada tenencia con {@code cantidad > 0} de este jugador
     * (ya sin reserva pendiente, tras {@link #cancelarOfertasAbiertasDe}) se pone a cero, su
     * usuario recibe saldo virtual equivalente a {@code cotizacionReferencia}, y queda registrado
     * un {@link Movimiento} {@code COMPENSACION_BAJA} — nunca se libera la emisión del jugador
     * (constitution.md §2, spec.md regla 9: a diferencia de una venta, estos tokens se retiran de
     * circulación de forma permanente).
     */
    private List<CompensacionBaja> compensarTenenciasDe(
            UUID jugadorId, BigDecimal cotizacionReferencia) {
        List<CompensacionBaja> compensaciones = new ArrayList<>();
        for (TenenciaTokenEntity tenenciaEntity :
                tenenciaTokenRepository.findByJugadorIdAndCantidadGreaterThan(jugadorId, 0)) {
            TenenciaToken tenencia = tenenciaEntity.aModelo();
            UUID usuarioId = tenencia.getUsuarioId();
            int cantidadRetirada = tenencia.getCantidad();
            tenencia.debitar(cantidadRetirada);
            tenenciaTokenRepository.save(TenenciaTokenEntity.desde(tenencia));

            Usuario usuario = obtenerUsuario(usuarioId);
            BigDecimal monto =
                    Monetario.escalar(
                            cotizacionReferencia.multiply(BigDecimal.valueOf(cantidadRetirada)));
            usuario.acreditarSaldo(monto);
            usuarioRepository.save(UsuarioEntity.desde(usuario));

            movimientoRepository.save(
                    MovimientoEntity.desde(
                            Movimiento.registrar(
                                    usuarioId,
                                    jugadorId,
                                    TipoMovimiento.COMPENSACION_BAJA,
                                    cantidadRetirada,
                                    cotizacionReferencia,
                                    monto,
                                    null)));

            compensaciones.add(new CompensacionBaja(usuarioId, cantidadRetirada, monto));
        }
        return compensaciones;
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

    private BigDecimal cotizacionVigenteDe(UUID jugadorId) {
        return cotizacionHistoricaRepository
                .findTopByJugadorIdOrderByFechaCalculoDesc(jugadorId)
                .map(CotizacionHistoricaEntity::getValor)
                .orElse(valorInicial);
    }

    private static DatosIdentificatoriosAuditoria datosIdentificatoriosDe(Jugador jugador) {
        return new DatosIdentificatoriosAuditoria(
                jugador.getNombre(),
                jugador.getClub(),
                jugador.getPosicion(),
                jugador.getFechaNacimiento(),
                jugador.getNacionalidad(),
                jugador.getLiga());
    }

    private record DatosIdentificatoriosAuditoria(
            String nombre,
            String club,
            String posicion,
            LocalDate fechaNacimiento,
            String nacionalidad,
            String liga) {}

    private record CompensacionBaja(UUID usuarioId, int cantidadCompensada, BigDecimal monto) {}

    private record BajaJugadorAuditoria(
            BigDecimal cotizacionReferencia, List<CompensacionBaja> compensaciones) {}
}
