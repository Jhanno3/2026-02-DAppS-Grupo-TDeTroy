package com.tdetroy.valuacion.dto.response;

import com.tdetroy.valuacion.model.Portfolio;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Respuesta de {@code GET /portfolios/me} (UC-11): tenencias con su valor a la cotización vigente,
 * precio promedio de compra y ganancia/pérdida, saldo virtual y valor total (spec.md UC-11: "la
 * suma del valor de todas las tenencias más el saldo virtual disponible"). {@code valorEnVenta} son
 * los tokens reservados en ofertas P2P abiertas y subastas activas, y {@code saldoEnPujas} el saldo
 * reservado como mejor postor de subastas: ninguno de los dos está disponible para operar, pero
 * ambos se incluyen en {@code valorTotal}. {@code gananciaPerdida} es el resultado total del
 * usuario: lo realizado en ventas más lo no realizado de sus tokens, reservados incluidos.
 */
public record PortfolioResponse(
        BigDecimal saldoVirtual,
        List<Tenencia> tenencias,
        BigDecimal valorEnVenta,
        BigDecimal saldoEnPujas,
        BigDecimal gananciaPerdida,
        BigDecimal valorTotal) {

    public static PortfolioResponse desde(Portfolio portfolio) {
        return new PortfolioResponse(
                portfolio.saldoVirtual(),
                portfolio.tenencias().stream().map(Tenencia::desde).toList(),
                portfolio.valorEnVenta(),
                portfolio.saldoEnPujas(),
                portfolio.gananciaPerdida(),
                portfolio.valorTotal());
    }

    /**
     * Tenencia de un jugador, valorizada a su cotización vigente. {@code gananciaPerdida} es {@code
     * valor} menos lo que costaron esos tokens, y {@code rentabilidad} ese resultado en porcentaje
     * sobre el costo ({@code 20.00} = +20%).
     */
    public record Tenencia(
            UUID jugadorId,
            int cantidad,
            BigDecimal cotizacionVigente,
            BigDecimal valor,
            BigDecimal precioPromedioCompra,
            BigDecimal gananciaPerdida,
            BigDecimal rentabilidad) {

        public static Tenencia desde(Portfolio.TenenciaPortfolio tenencia) {
            return new Tenencia(
                    tenencia.jugadorId(),
                    tenencia.cantidad(),
                    tenencia.cotizacionVigente(),
                    tenencia.valor(),
                    tenencia.precioPromedioCompra(),
                    tenencia.gananciaPerdida(),
                    tenencia.rentabilidad());
        }
    }
}
