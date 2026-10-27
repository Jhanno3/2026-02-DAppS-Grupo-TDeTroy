package com.tdetroy.valuacion.model;

/**
 * Fuente que aportó un {@link RendimientoPartido} (plan.md §2.3/§8.1).
 *
 * <ul>
 *   <li>{@code FOOTBALL_DATA}: resultado/alineación/fixture confirmado por Football-Data.org, con
 *       {@code metricas} vacío (ver {@code RendimientoServiceImpl#ingestarFixtures}).
 *   <li>{@code WHOSCORED_LIGA}: delta semanal de métricas acumuladas de temporada, resuelto desde
 *       la página pública de estadísticas de liga de WhoScored (ver {@code
 *       RendimientoServiceImpl#ingestarRendimiento}) — reemplaza al scraping por jugador que
 *       existía antes de esta fuente.
 * </ul>
 */
public enum FuenteResultado {
    FOOTBALL_DATA,
    WHOSCORED_LIGA
}
