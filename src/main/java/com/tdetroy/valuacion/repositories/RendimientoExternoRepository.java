package com.tdetroy.valuacion.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto hacia la fuente de rendimiento detallado por jugador (WhoScored, scraping) — plan.md §8.1,
 * constitution.md §1: el Service que la consuma ({@code RendimientoService}, tasks.md T3.5) nunca
 * conoce el formato de respuesta de WhoScored, sólo esta interfaz, {@link RendimientoCrudo} y la
 * URL de foto que traduce {@link #obtenerFotoUrl}, modelos de transporte que cada implementación
 * traduce desde su propio formato externo.
 *
 * <p>Implementada por {@link WhoScoredRepositoryImpl} (tasks.md T3.3).
 */
public interface RendimientoExternoRepository {

    /**
     * Rendimiento detallado de {@code jugadorId} en los partidos que disputó entre {@code desde} y
     * {@code hasta} (ambos inclusive).
     */
    List<RendimientoCrudo> obtenerRendimiento(UUID jugadorId, LocalDate desde, LocalDate hasta);

    /**
     * URL de la foto de perfil de {@code jugadorId} tal como la publica la fuente externa. {@link
     * Optional#empty()} si la fuente no tiene (o no pudo servir) una foto para ese jugador — nunca
     * lanza, misma política de aislamiento de errores que {@link #obtenerRendimiento}.
     */
    Optional<String> obtenerFotoUrl(UUID jugadorId);
}
