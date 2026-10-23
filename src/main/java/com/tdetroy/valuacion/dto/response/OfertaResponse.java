package com.tdetroy.valuacion.dto.response;

import com.tdetroy.valuacion.model.EstadoOferta;
import com.tdetroy.valuacion.model.OfertaP2P;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Respuesta de {@code POST /ofertas} y de un elemento de {@code GET /ofertas} (con o sin {@code
 * jugadorId}, UC-17). {@code precioUnitario} es el precio fijado libremente por el vendedor al
 * publicarla (fijo durante toda su vida); {@code fechaVencimiento} es cuándo se cancela
 * automáticamente si nadie la compró.
 */
public record OfertaResponse(
        UUID id,
        UUID vendedorId,
        UUID jugadorId,
        int cantidadPublicada,
        int cantidadDisponible,
        BigDecimal precioUnitario,
        EstadoOferta estado,
        Instant fechaCreacion,
        Instant fechaVencimiento) {

    public static OfertaResponse desde(OfertaP2P oferta) {
        return new OfertaResponse(
                oferta.getId(),
                oferta.getVendedorId(),
                oferta.getJugadorId(),
                oferta.getCantidadPublicada(),
                oferta.getCantidadDisponible(),
                oferta.getPrecioUnitario(),
                oferta.getEstado(),
                oferta.getFechaCreacion(),
                oferta.getFechaVencimiento());
    }
}
