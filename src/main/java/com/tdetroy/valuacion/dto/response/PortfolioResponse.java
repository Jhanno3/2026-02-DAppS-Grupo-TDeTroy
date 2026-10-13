package com.tdetroy.valuacion.dto.response;

import com.tdetroy.valuacion.services.PortfolioService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Respuesta de {@code GET /portfolios/me} (UC-11): tenencias con su valor a la cotización vigente,
 * saldo virtual y valor total (spec.md UC-11: "la suma del valor de todas las tenencias más el
 * saldo virtual disponible").
 */
public record PortfolioResponse(
        BigDecimal saldoVirtual, List<Tenencia> tenencias, BigDecimal valorTotal) {

    public static PortfolioResponse desde(PortfolioService.Portfolio portfolio) {
        return new PortfolioResponse(
                portfolio.saldoVirtual(),
                portfolio.tenencias().stream().map(Tenencia::desde).toList(),
                portfolio.valorTotal());
    }

    /** Tenencia de un jugador, valorizada a su cotización vigente. */
    public record Tenencia(
            UUID jugadorId, int cantidad, BigDecimal cotizacionVigente, BigDecimal valor) {

        public static Tenencia desde(PortfolioService.TenenciaPortfolio tenencia) {
            return new Tenencia(
                    tenencia.jugadorId(),
                    tenencia.cantidad(),
                    tenencia.cotizacionVigente(),
                    tenencia.valor());
        }
    }
}
