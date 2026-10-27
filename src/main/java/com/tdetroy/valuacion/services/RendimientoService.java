package com.tdetroy.valuacion.services;

/**
 * Ingesta semanal de rendimiento (UC-05, plan.md §1/§8). Orquesta las fuentes externas aisladas
 * detrás de {@code repositories/} ({@code FixtureRepository}, {@code
 * RendimientoLigaExternoRepository}, {@code RendimientoExternoRepository}) y persiste el resultado
 * como {@code model/RendimientoPartido} — nunca conoce el formato de respuesta de Football-Data.org
 * ni de WhoScored (constitution.md §1/§2).
 *
 * <p>Pensada para ser invocada directamente desde un componente de scheduling en {@code config/}
 * (tasks.md T3.6), nunca desde un Controller: ninguno de los dos métodos es un disparador HTTP.
 */
public interface RendimientoService {

    /**
     * Confirma, vía {@code FixtureRepository}, los partidos ya disputados por los clubes del
     * catálogo en la semana en curso (lunes de esta semana hasta hoy) y persiste un {@code
     * RendimientoPartido} por cada jugador {@code ACTIVO} cuyo club disputó alguno, con {@code
     * metricas} vacío — WhoScored todavía no aportó el detalle (plan.md §8.2: pensada para correr
     * antes que {@link #ingestarRendimiento()} dentro del mismo ciclo semanal).
     */
    void ingestarFixtures();

    /**
     * Para cada liga presente en el catálogo {@code ACTIVO}, obtiene vía {@code
     * RendimientoLigaExternoRepository} el acumulado de temporada de todos sus jugadores listados
     * en la página de liga de WhoScored, resuelve cada fila a un jugador del catálogo por
     * nombre+club, y calcula el delta semanal contra el último snapshot conocido de ese jugador
     * (plan.md §8.1): ese delta (nunca negativo) es lo que persiste como {@code
     * RendimientoPartido}, actualizando la fecha de última actualización de rendimiento del jugador
     * (UC-07). También refresca, vía {@code RendimientoExternoRepository}, la foto de perfil de
     * cada jugador {@code ACTIVO} — no es rendimiento deportivo, pero sale de la misma fuente y
     * comparte la misma cadencia.
     */
    void ingestarRendimiento();
}
