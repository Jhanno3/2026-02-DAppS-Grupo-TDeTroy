package com.tdetroy.valuacion.repositories;

import java.time.LocalDate;
import java.util.List;

/**
 * Puerto hacia la fuente de fixtures/resultados/alineaciones (Football-Data.org, API oficial) —
 * plan.md §8.1, constitution.md §1: el Service que la consuma ({@code RendimientoService}, tasks.md
 * T3.5) nunca conoce el formato de respuesta de Football-Data.org, sólo esta interfaz y {@link
 * PartidoCrudo}, el modelo de transporte que {@link FootballDataRepositoryImpl} traduce desde su
 * propio formato externo (T3.4).
 */
public interface FixtureRepository {

    /**
     * Partidos ya disputados (estado {@code FINISHED}) entre {@code desde} y {@code hasta} (ambos
     * inclusive).
     */
    List<PartidoCrudo> obtenerPartidosDisputados(LocalDate desde, LocalDate hasta);
}
