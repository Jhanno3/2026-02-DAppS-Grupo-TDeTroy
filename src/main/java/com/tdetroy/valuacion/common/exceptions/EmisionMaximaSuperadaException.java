package com.tdetroy.valuacion.common.exceptions;

/**
 * La emisión de tokens de un jugador superaría el máximo de 100 unidades.
 *
 * <p>Lanzada por {@code Jugador.emitirTokens} (plan.md §2.2, constitution.md §2: invariante de
 * emisión no negociable, protegida en la propia entidad y no sólo en {@code JugadorService}/ {@code
 * TokenService}, bajo ninguna operación incluidas las administrativas).
 */
public final class EmisionMaximaSuperadaException extends NegocioException {

    public EmisionMaximaSuperadaException(
            int tokensEmitidos, int cantidadSolicitada, int maximoPermitido) {
        super(
                "Emisión máxima superada: %d tokens ya emitidos + %d solicitados supera el máximo de %d"
                        .formatted(tokensEmitidos, cantidadSolicitada, maximoPermitido));
    }
}
