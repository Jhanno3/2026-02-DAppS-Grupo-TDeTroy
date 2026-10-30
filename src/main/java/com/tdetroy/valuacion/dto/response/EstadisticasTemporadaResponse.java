package com.tdetroy.valuacion.dto.response;

import com.tdetroy.valuacion.model.EstadisticasTemporada;
import java.time.Instant;

/** Respuesta de {@code GET /jugadores/{id}/estadisticas} (UC-07) — acumulado de temporada. */
public record EstadisticasTemporadaResponse(
        int partidosJugados,
        int minutosJugados,
        int goles,
        int asistencias,
        int figuraDelPartido,
        int tarjetasAmarillas,
        int tarjetasRojas,
        Instant fechaActualizacion) {

    public static EstadisticasTemporadaResponse desde(EstadisticasTemporada estadisticas) {
        return new EstadisticasTemporadaResponse(
                estadisticas.partidosJugados(),
                estadisticas.minutosJugados(),
                estadisticas.goles(),
                estadisticas.asistencias(),
                estadisticas.figuraDelPartido(),
                estadisticas.tarjetasAmarillas(),
                estadisticas.tarjetasRojas(),
                estadisticas.fechaActualizacion());
    }
}
