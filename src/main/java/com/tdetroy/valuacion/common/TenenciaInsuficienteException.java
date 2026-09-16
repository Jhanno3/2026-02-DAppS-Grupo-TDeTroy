package com.tdetroy.valuacion.common;

/**
 * La cantidad {@code disponible()} de tokens de un usuario para un jugador no alcanza para la
 * operación solicitada.
 *
 * <p>Lanzada por {@code TenenciaToken.debitar}/{@code reservar} (plan.md §2.5, UC-10/UC-17) —
 * la invariante {@code cantidadReservada ≤ cantidad} vive en esos métodos propios de la
 * entidad, no sólo en el Service (constitution.md §2).
 */
public final class TenenciaInsuficienteException extends NegocioException {

    public TenenciaInsuficienteException(int cantidadSolicitada, int cantidadDisponible) {
        super("Tenencia insuficiente: se solicitaron %d tokens pero hay %d disponibles"
                .formatted(cantidadSolicitada, cantidadDisponible));
    }
}
