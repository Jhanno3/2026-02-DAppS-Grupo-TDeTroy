package com.tdetroy.valuacion.model.rendimiento;

import java.time.Instant;

/** Vista tipada del último {@link RendimientoAcumuladoWhoScored}; no se persiste. */
public record EstadisticasTemporada(
        int partidosJugados,
        int minutosJugados,
        int goles,
        int asistencias,
        int figuraDelPartido,
        int tarjetasAmarillas,
        int tarjetasRojas,
        Instant fechaActualizacion) {}
