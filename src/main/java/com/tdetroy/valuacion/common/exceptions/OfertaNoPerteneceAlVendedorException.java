package com.tdetroy.valuacion.common.exceptions;

import java.util.UUID;

/**
 * Un usuario intentó cancelar una {@code OfertaP2P} publicada por otro vendedor (plan.md §9.4,
 * UC-17: "sólo el vendedor dueño").
 */
public final class OfertaNoPerteneceAlVendedorException extends NegocioException {

    public OfertaNoPerteneceAlVendedorException(UUID ofertaId, UUID usuarioId) {
        super("La oferta %s no pertenece al usuario %s".formatted(ofertaId, usuarioId));
    }
}
