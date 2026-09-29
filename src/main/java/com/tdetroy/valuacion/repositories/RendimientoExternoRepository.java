package com.tdetroy.valuacion.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Puerto hacia la fuente de rendimiento detallado por jugador (WhoScored, scraping) — plan.md §8.1,
 * constitution.md §1: el Service que la consuma ({@code RendimientoService}, tasks.md T3.5) nunca
 * conoce el formato de respuesta de WhoScored, sólo esta interfaz y {@link RendimientoCrudo}, el
 * modelo de transporte que cada implementación traduce desde su propio formato externo.
 *
 * <p>Deliberadamente sin implementación todavía ({@code WhoScoredRepositoryImpl}, T3.3, fuera de
 * alcance de esta tarea).
 */
public interface RendimientoExternoRepository {

    /**
     * Rendimiento detallado de {@code jugadorId} en los partidos que disputó entre {@code desde} y
     * {@code hasta} (ambos inclusive).
     */
    List<RendimientoCrudo> obtenerRendimiento(UUID jugadorId, LocalDate desde, LocalDate hasta);
}
