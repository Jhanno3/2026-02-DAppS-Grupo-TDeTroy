package com.tdetroy.valuacion.services;

import com.tdetroy.valuacion.common.Monetario;
import com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException;
import com.tdetroy.valuacion.entity.CotizacionHistoricaEntity;
import com.tdetroy.valuacion.entity.TenenciaTokenEntity;
import com.tdetroy.valuacion.entity.UsuarioEntity;
import com.tdetroy.valuacion.repositories.CotizacionHistoricaRepository;
import com.tdetroy.valuacion.repositories.TenenciaTokenRepository;
import com.tdetroy.valuacion.repositories.UsuarioRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** {@inheritDoc} */
@Service
public class PortfolioServiceImpl implements PortfolioService {

    private static final String ENTIDAD_USUARIO = "Usuario";

    private final UsuarioRepository usuarioRepository;
    private final TenenciaTokenRepository tenenciaTokenRepository;
    private final CotizacionHistoricaRepository cotizacionHistoricaRepository;
    private final BigDecimal valorInicial;

    public PortfolioServiceImpl(
            UsuarioRepository usuarioRepository,
            TenenciaTokenRepository tenenciaTokenRepository,
            CotizacionHistoricaRepository cotizacionHistoricaRepository,
            @Value("${app.cotizacion.valor-inicial}") BigDecimal valorInicial) {
        this.usuarioRepository = usuarioRepository;
        this.tenenciaTokenRepository = tenenciaTokenRepository;
        this.cotizacionHistoricaRepository = cotizacionHistoricaRepository;
        this.valorInicial = valorInicial;
    }

    @Override
    public Portfolio obtenerDe(UUID usuarioId) {
        UsuarioEntity usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(
                                () -> new RecursoNoEncontradoException(ENTIDAD_USUARIO, usuarioId));

        List<TenenciaPortfolio> tenencias =
                tenenciaTokenRepository.findByUsuarioIdAndCantidadGreaterThan(usuarioId, 0).stream()
                        .map(this::valorizar)
                        .toList();

        BigDecimal valorTenencias =
                tenencias.stream()
                        .map(TenenciaPortfolio::valor)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal valorTotal = Monetario.escalar(usuario.getSaldoVirtual().add(valorTenencias));

        return new Portfolio(usuario.getSaldoVirtual(), tenencias, valorTotal);
    }

    private TenenciaPortfolio valorizar(TenenciaTokenEntity tenencia) {
        BigDecimal cotizacionVigente = cotizacionVigenteDe(tenencia.getJugadorId());
        BigDecimal valor =
                Monetario.escalar(
                        cotizacionVigente.multiply(BigDecimal.valueOf(tenencia.getCantidad())));
        return new TenenciaPortfolio(
                tenencia.getJugadorId(), tenencia.getCantidad(), cotizacionVigente, valor);
    }

    private BigDecimal cotizacionVigenteDe(UUID jugadorId) {
        return cotizacionHistoricaRepository
                .findTopByJugadorIdOrderByFechaCalculoDesc(jugadorId)
                .map(CotizacionHistoricaEntity::getValor)
                .orElse(valorInicial);
    }
}
