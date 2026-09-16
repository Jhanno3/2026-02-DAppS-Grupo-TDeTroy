package com.tdetroy.valuacion.common.exceptions;

import java.math.BigDecimal;

/**
 * El saldo virtual disponible de un usuario no alcanza para el monto solicitado.
 *
 * <p>Lanzada por {@code Usuario.debitarSaldo} (plan.md §2.1, UC-09/UC-10/UC-17) — nunca
 * validada sólo en el Service que la invoca (constitution.md §2).
 */
public final class SaldoInsuficienteException extends NegocioException {

    public SaldoInsuficienteException(BigDecimal montoSolicitado, BigDecimal saldoDisponible) {
        super("Saldo insuficiente: se solicitó débito de %s pero el saldo disponible es %s"
                .formatted(montoSolicitado, saldoDisponible));
    }
}
