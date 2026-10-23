package com.tdetroy.valuacion.common.exceptions;

import com.tdetroy.valuacion.model.EstadoOferta;
import java.util.UUID;

/**
 * Se intentó cancelar una {@code OfertaP2P} que ya no está {@link EstadoOferta#ABIERTA} (plan.md
 * §9.4, UC-17: "si la oferta ya está AGOTADA o CANCELADA, se rechaza").
 */
public final class OfertaNoAbiertaException extends NegocioException {

    public OfertaNoAbiertaException(UUID ofertaId, EstadoOferta estadoActual) {
        super("La oferta %s no está abierta (estado actual: %s)".formatted(ofertaId, estadoActual));
    }
}
