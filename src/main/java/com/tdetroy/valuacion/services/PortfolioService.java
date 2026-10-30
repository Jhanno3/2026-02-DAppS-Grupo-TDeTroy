package com.tdetroy.valuacion.services;

import com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Vista de lectura del portfolio de un usuario (plan.md §1/§2.5, UC-11), combinando {@code
 * TenenciaTokenRepository} y {@code CotizacionHistoricaRepository} — sin entidad {@code Portfolio}
 * propia persistida (plan.md §2.5: "base de lectura del portfolio; no existe una entidad Portfolio
 * separada"). {@link Portfolio}/{@link TenenciaPortfolio} son records de sólo lectura de este
 * Service, nunca una tabla ni un {@code model/} con invariantes propias.
 */
public interface PortfolioService {

    /**
     * Portfolio de {@code usuarioId}: sus tenencias con tokens disponibles ({@code cantidad -
     * cantidadReservada > 0}) — lo reservado en una oferta P2P abierta (UC-17) queda fuera hasta
     * que la oferta se cancele o venza y la reserva se libere —, cada una valorizada a la
     * cotización vigente del jugador correspondiente (o {@code app.cotizacion.valor-inicial} si ese
     * jugador todavía no tiene ningún cálculo persistido), más el saldo virtual y el valor total
     * (spec.md UC-11: "la suma del valor de todas las tenencias más el saldo virtual disponible").
     * Un usuario sin tenencias ve una lista vacía, nunca un error.
     *
     * @throws RecursoNoEncontradoException si no existe {@code usuarioId}
     */
    Portfolio obtenerDe(UUID usuarioId);

    /**
     * Tenencia de un jugador valorizada a su cotización vigente, para {@link #obtenerDe}. {@code
     * cantidad} es lo disponible (sin lo reservado en ofertas P2P abiertas), y {@code valor} se
     * calcula sobre esa cantidad.
     */
    record TenenciaPortfolio(
            UUID jugadorId, int cantidad, BigDecimal cotizacionVigente, BigDecimal valor) {}

    /**
     * {@code valorEnVenta} = tokens reservados en ofertas P2P abiertas, valorizados a la cotización
     * vigente: no están en la cartera, pero siguen siendo patrimonio del usuario (si se venden se
     * cobra; si vencen o se cancelan, el token vuelve) — incluye los tokens reservados en subastas
     * activas (UC-18). {@code saldoEnPujas} = saldo descontado al usuario como mejor postor de
     * subastas activas, que le pertenece hasta que se adjudique. {@code valorTotal} = suma de
     * {@code tenencias[].valor} + {@code valorEnVenta} + {@code saldoEnPujas} + {@code
     * saldoVirtual} (spec.md UC-11).
     */
    record Portfolio(
            BigDecimal saldoVirtual,
            List<TenenciaPortfolio> tenencias,
            BigDecimal valorEnVenta,
            BigDecimal saldoEnPujas,
            BigDecimal valorTotal) {}
}
