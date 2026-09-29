package com.tdetroy.valuacion.repositories;

import java.time.LocalDate;

/**
 * Partido crudo tal como lo traduce {@link FootballDataRepositoryImpl} desde el formato propio de
 * Football-Data.org (plan.md §8.1) — resultado/fixture de un partido ya disputado, sin ningún
 * jugador asociado todavía: relacionar cada partido con los jugadores del catálogo que lo
 * disputaron es responsabilidad de {@code RendimientoService} (tasks.md T3.5), no de esta
 * traducción.
 */
public record PartidoCrudo(
        String partidoExternoId,
        LocalDate fecha,
        String estado,
        String equipoLocal,
        String equipoVisitante,
        Integer golesLocal,
        Integer golesVisitante) {}
