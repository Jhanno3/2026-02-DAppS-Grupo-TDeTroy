package com.tdetroy.valuacion.dto.cotizacion;

import com.tdetroy.valuacion.model.cotizacion.CotizacionHistorica;
import com.tdetroy.valuacion.model.cotizacion.EstrategiaCotizacion;
import com.tdetroy.valuacion.model.cotizacion.OrigenCotizacion;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Respuesta de {@code GET /jugadores/{id}/cotizaciones} (UC-08, tasks.md T4.7) -- un registro del
 * historial cronologico de un jugador.
 */
public record CotizacionResponse(
        UUID id,
        String semana,
        BigDecimal valor,
        OrigenCotizacion origen,
        EstrategiaCotizacion estrategia,
        Instant fechaCalculo) {

    public static CotizacionResponse desde(CotizacionHistorica cotizacion) {
        return new CotizacionResponse(
                cotizacion.getId(),
                cotizacion.getSemana(),
                cotizacion.getValor(),
                cotizacion.getOrigen(),
                cotizacion.getEstrategia(),
                cotizacion.getFechaCalculo());
    }
}
