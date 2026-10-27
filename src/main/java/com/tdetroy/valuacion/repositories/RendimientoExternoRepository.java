package com.tdetroy.valuacion.repositories;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto hacia la foto de perfil de un jugador en WhoScored (plan.md §8.1). Las métricas de
 * rendimiento en sí se obtienen ahora vía {@link RendimientoLigaExternoRepository} (ingesta por
 * liga completa, no por jugador) — este puerto quedó acotado a la foto, que sigue siendo
 * por-jugador porque la página de liga no la provee.
 *
 * <p>Implementada por {@link WhoScoredRepositoryImpl}.
 */
public interface RendimientoExternoRepository {

    /**
     * URL de la foto de perfil de {@code jugadorId} tal como la publica la fuente externa. {@link
     * Optional#empty()} si la fuente no tiene (o no pudo servir) una foto para ese jugador — nunca
     * lanza, mismo criterio de aislamiento que el resto de los repositories de fuentes externas
     * (constitution.md §1).
     */
    Optional<String> obtenerFotoUrl(UUID jugadorId);
}
