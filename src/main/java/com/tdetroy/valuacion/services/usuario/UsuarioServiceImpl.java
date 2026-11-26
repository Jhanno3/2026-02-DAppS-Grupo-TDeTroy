package com.tdetroy.valuacion.services.usuario;

import com.tdetroy.valuacion.common.Monetario;
import com.tdetroy.valuacion.common.exceptions.EmailYaRegistradoException;
import com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException;
import com.tdetroy.valuacion.entity.cartera.MovimientoEntity;
import com.tdetroy.valuacion.entity.usuario.UsuarioEntity;
import com.tdetroy.valuacion.model.cartera.Movimiento;
import com.tdetroy.valuacion.model.cartera.TipoMovimiento;
import com.tdetroy.valuacion.model.usuario.Usuario;
import com.tdetroy.valuacion.repositories.cartera.MovimientoRepository;
import com.tdetroy.valuacion.repositories.usuario.UsuarioRepository;
import com.tdetroy.valuacion.services.auditoria.AuditoriaService;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** {@inheritDoc} */
@Service
public class UsuarioServiceImpl implements UsuarioService {

    private static final String ENTIDAD_AUDITADA = "Usuario";

    private final UsuarioRepository usuarioRepository;
    private final MovimientoRepository movimientoRepository;
    private final AuditoriaService auditoriaService;
    private final PasarelaPagos pasarelaPagos;

    public UsuarioServiceImpl(
            UsuarioRepository usuarioRepository,
            MovimientoRepository movimientoRepository,
            AuditoriaService auditoriaService,
            PasarelaPagos pasarelaPagos) {
        this.usuarioRepository = usuarioRepository;
        this.movimientoRepository = movimientoRepository;
        this.auditoriaService = auditoriaService;
        this.pasarelaPagos = pasarelaPagos;
    }

    @Override
    @Transactional
    public Usuario registrar(String email, String passwordHash) {
        if (usuarioRepository.existsByEmail(email)) {
            throw new EmailYaRegistradoException(email);
        }
        return usuarioRepository
                .save(UsuarioEntity.desde(Usuario.registrar(email, passwordHash)))
                .aModelo();
    }

    @Override
    @Transactional
    public Usuario recargarSaldo(UUID usuarioId, BigDecimal monto, UUID actorId) {
        Usuario usuario = acreditarYRegistrarMovimiento(usuarioId, monto);

        auditoriaService.registrar(
                actorId,
                "RECARGA_SALDO",
                ENTIDAD_AUDITADA,
                usuarioId,
                new SaldoAuditoria(usuario.getSaldoVirtual().subtract(Monetario.escalar(monto))),
                new SaldoAuditoria(usuario.getSaldoVirtual()));

        return usuario;
    }

    @Override
    @Transactional
    public Usuario cargarSaldoPropio(
            UUID usuarioId, BigDecimal monto, PasarelaPagos.DatosTarjeta tarjeta) {
        PasarelaPagos.ResultadoPago pago = pasarelaPagos.procesarPago(monto, tarjeta);
        Usuario usuario = acreditarYRegistrarMovimiento(usuarioId, monto);

        auditoriaService.registrar(
                usuarioId,
                "CARGA_SALDO_PASARELA",
                ENTIDAD_AUDITADA,
                usuarioId,
                new SaldoAuditoria(usuario.getSaldoVirtual().subtract(Monetario.escalar(monto))),
                new SaldoPasarelaAuditoria(
                        usuario.getSaldoVirtual(),
                        pago.codigoOperacion(),
                        pago.ultimosCuatroDigitos()));

        return usuario;
    }

    private Usuario acreditarYRegistrarMovimiento(UUID usuarioId, BigDecimal monto) {
        Usuario usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .map(UsuarioEntity::aModelo)
                        .orElseThrow(
                                () ->
                                        new RecursoNoEncontradoException(
                                                ENTIDAD_AUDITADA, usuarioId));

        usuario.acreditarSaldo(monto);
        usuarioRepository.save(UsuarioEntity.desde(usuario));
        BigDecimal montoEscalado = Monetario.escalar(monto);

        movimientoRepository.save(
                MovimientoEntity.desde(
                        Movimiento.registrar(
                                usuarioId,
                                null,
                                TipoMovimiento.RECARGA_SALDO,
                                null,
                                null,
                                montoEscalado,
                                null)));
        return usuario;
    }

    private record SaldoAuditoria(BigDecimal saldoVirtual) {}

    private record SaldoPasarelaAuditoria(
            BigDecimal saldoVirtual, String codigoOperacion, String ultimosCuatroDigitos) {}
}
