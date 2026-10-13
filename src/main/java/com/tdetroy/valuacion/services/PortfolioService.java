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
     * Portfolio de {@code usuarioId}: sus tenencias con {@code cantidad > 0}, cada una valorizada a
     * la cotización vigente del jugador correspondiente (o {@code app.cotizacion.valor-inicial} si
     * ese jugador todavía no tiene ningún cálculo persistido), más el saldo virtual y el valor
     * total (spec.md UC-11: "la suma del valor de todas las tenencias más el saldo virtual
     * disponible"). Un usuario sin tenencias ve una lista vacía, nunca un error.
     *
     * @throws RecursoNoEncontradoException si no existe {@code usuarioId}
     */
    Portfolio obtenerDe(UUID usuarioId);

    /** Tenencia de un jugador valorizada a su cotización vigente, para {@link #obtenerDe}. */
    record TenenciaPortfolio(
            UUID jugadorId, int cantidad, BigDecimal cotizacionVigente, BigDecimal valor) {}

    /**
     * {@code valorTotal} = suma de {@code tenencias[].valor} + {@code saldoVirtual} (spec.md
     * UC-11).
     */
    record Portfolio(
            BigDecimal saldoVirtual, List<TenenciaPortfolio> tenencias, BigDecimal valorTotal) {}
}
