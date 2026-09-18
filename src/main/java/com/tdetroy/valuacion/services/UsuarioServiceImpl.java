package com.tdetroy.valuacion.services;

import com.tdetroy.valuacion.common.Monetario;
import com.tdetroy.valuacion.common.exceptions.EmailYaRegistradoException;
import com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException;
import com.tdetroy.valuacion.model.Movimiento;
import com.tdetroy.valuacion.model.TipoMovimiento;
import com.tdetroy.valuacion.model.Usuario;
import com.tdetroy.valuacion.repositories.MovimientoRepository;
import com.tdetroy.valuacion.repositories.UsuarioRepository;
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

    public UsuarioServiceImpl(
            UsuarioRepository usuarioRepository,
            MovimientoRepository movimientoRepository,
            AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.movimientoRepository = movimientoRepository;
        this.auditoriaService = auditoriaService;
    }

    @Override
    @Transactional
    public Usuario registrar(String email, String passwordHash) {
        if (usuarioRepository.existsByEmail(email)) {
            throw new EmailYaRegistradoException(email);
        }
        return usuarioRepository.save(Usuario.registrar(email, passwordHash));
    }

    @Override
    @Transactional
    public Usuario recargarSaldo(UUID usuarioId, BigDecimal monto, UUID actorId) {
        Usuario usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(
                                () ->
                                        new RecursoNoEncontradoException(
                                                ENTIDAD_AUDITADA, usuarioId));

        BigDecimal saldoAntes = usuario.getSaldoVirtual();
        usuario.acreditarSaldo(monto);
        BigDecimal montoEscalado = Monetario.escalar(monto);

        movimientoRepository.save(
                Movimiento.registrar(
                        usuarioId,
                        null,
                        TipoMovimiento.RECARGA_SALDO,
                        null,
                        null,
                        montoEscalado,
                        null));

        auditoriaService.registrar(
                actorId,
                "RECARGA_SALDO",
                ENTIDAD_AUDITADA,
                usuarioId,
                new SaldoAuditoria(saldoAntes),
                new SaldoAuditoria(usuario.getSaldoVirtual()));

        return usuario;
    }

    private record SaldoAuditoria(BigDecimal saldoVirtual) {}
}
