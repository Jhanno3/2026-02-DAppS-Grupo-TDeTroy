package com.tdetroy.valuacion.repositories;

import java.util.Map;

/**
 * Fila cruda de la tabla de jugadores de la página pública de liga de WhoScored (plan.md §8.1), tal
 * como la traduce {@link WhoScoredLigaRepositoryImpl} — todavía sin resolver a un {@code Jugador}
 * del catálogo: eso es responsabilidad de {@code RendimientoServiceImpl}, vía matching por
 * nombre+club (ver {@code services.NombreClubMatcher}).
 *
 * <p>{@code metricas} llega tal cual la tabla la expone (una entrada por {@code td[data-metrica]}),
 * sin curar — mismo criterio de traducción genérica que tenía {@code RendimientoCrudo} (eliminado):
 * la curación al conjunto permitido de métricas acumuladas de temporada
 * (Apps/Goals/Assists/ManOfTheMatch/YellowCard/RedCard/MinsPlayed) ocurre en {@code
 * RendimientoServiceImpl}, no acá.
 */
public record JugadorRendimientoLigaCrudo(
        String nombre, String club, Map<String, Object> metricas) {}
