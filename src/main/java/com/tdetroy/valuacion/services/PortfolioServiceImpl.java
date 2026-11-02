package com.tdetroy.valuacion.services;

import com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException;
import com.tdetroy.valuacion.entity.CotizacionHistoricaEntity;
import com.tdetroy.valuacion.entity.SubastaEntity;
import com.tdetroy.valuacion.entity.TenenciaTokenEntity;
import com.tdetroy.valuacion.entity.UsuarioEntity;
import com.tdetroy.valuacion.model.EstadoSubasta;
import com.tdetroy.valuacion.model.Portfolio;
import com.tdetroy.valuacion.model.Subasta;
import com.tdetroy.valuacion.model.TenenciaToken;
import com.tdetroy.valuacion.repositories.CotizacionHistoricaRepository;
import com.tdetroy.valuacion.repositories.SubastaRepository;
import com.tdetroy.valuacion.repositories.TenenciaTokenRepository;
import com.tdetroy.valuacion.repositories.UsuarioRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final SubastaRepository subastaRepository;
    private final BigDecimal valorInicial;

    public PortfolioServiceImpl(
            UsuarioRepository usuarioRepository,
            TenenciaTokenRepository tenenciaTokenRepository,
            CotizacionHistoricaRepository cotizacionHistoricaRepository,
            SubastaRepository subastaRepository,
            @Value("${app.cotizacion.valor-inicial}") BigDecimal valorInicial) {
        this.usuarioRepository = usuarioRepository;
        this.tenenciaTokenRepository = tenenciaTokenRepository;
        this.cotizacionHistoricaRepository = cotizacionHistoricaRepository;
        this.subastaRepository = subastaRepository;
        this.valorInicial = valorInicial;
    }

    @Override
    public Portfolio obtenerDe(UUID usuarioId) {
        UsuarioEntity usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(
                                () -> new RecursoNoEncontradoException(ENTIDAD_USUARIO, usuarioId));

        List<TenenciaToken> tenencias =
                tenenciaTokenRepository.findByUsuarioIdAndCantidadGreaterThan(usuarioId, 0).stream()
                        .map(TenenciaTokenEntity::aModelo)
                        .toList();
        Map<UUID, BigDecimal> cotizacionesVigentes = new HashMap<>();
        for (TenenciaToken tenencia : tenencias) {
            cotizacionesVigentes.put(
                    tenencia.getJugadorId(), cotizacionVigenteDe(tenencia.getJugadorId()));
        }
        // UC-18: el saldo reservado como mejor postor de subastas activas ya no está en
        // saldoVirtual (se descontó al pujar), pero sigue siendo del usuario hasta que se
        // adjudique.
        List<Subasta> subastasComoMejorPostor =
                subastaRepository
                        .findByMejorPostorIdAndEstado(usuarioId, EstadoSubasta.ACTIVA)
                        .stream()
                        .map(SubastaEntity::aModelo)
                        .toList();

        return Portfolio.valorizar(
                usuario.getSaldoVirtual(),
                tenencias,
                cotizacionesVigentes,
                subastasComoMejorPostor);
    }

    private BigDecimal cotizacionVigenteDe(UUID jugadorId) {
        return cotizacionHistoricaRepository
                .findTopByJugadorIdOrderByFechaCalculoDesc(jugadorId)
                .map(CotizacionHistoricaEntity::getValor)
                .orElse(valorInicial);
    }
}
