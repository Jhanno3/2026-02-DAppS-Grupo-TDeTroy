package com.tdetroy.valuacion.dto.response;

import com.tdetroy.valuacion.model.CotizacionHistorica;
import com.tdetroy.valuacion.model.OrigenCotizacion;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Respuesta de {@code GET /jugadores/{id}/cotizaciones} (UC-08, tasks.md T4.7) -- un registro del
 * historial cronologico de un jugador.
 */
public record CotizacionResponse(
        UUID id, String semana, BigDecimal valor, OrigenCotizacion origen, Instant fechaCalculo) {

    public static CotizacionResponse desde(CotizacionHistorica cotizacion) {
        return new CotizacionResponse(
                cotizacion.getId(),
                cotizacion.getSemana(),
                cotizacion.getValor(),
                cotizacion.getOrigen(),
                cotizacion.getFechaCalculo());
    }
}
