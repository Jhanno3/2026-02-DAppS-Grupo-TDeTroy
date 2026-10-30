package com.tdetroy.valuacion.model;

import java.time.Instant;

/**
 * Acumulado de temporada de un jugador tal como lo expone la página de liga de WhoScored (plan.md
 * §8.1) — vista de sólo lectura sobre el último {@link RendimientoAcumuladoWhoScored} conocido, ya
 * parseado a campos tipados para la pantalla de detalle (UC-07). Nunca se persiste por su cuenta.
 */
public record EstadisticasTemporada(
        int partidosJugados,
        int minutosJugados,
        int goles,
        int asistencias,
        int figuraDelPartido,
        int tarjetasAmarillas,
        int tarjetasRojas,
        Instant fechaActualizacion) {}
