package com.tdetroy.valuacion.services;

/**
 * Ingesta semanal de rendimiento (UC-05, plan.md §1/§8). Orquesta las dos fuentes externas aisladas
 * detrás de {@code repositories/} ({@code FixtureRepository}, {@code RendimientoExternoRepository})
 * y persiste el resultado como {@code model/RendimientoPartido} — nunca conoce el formato de
 * respuesta de Football-Data.org ni de WhoScored (constitution.md §1/§2).
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
     * Obtiene, vía {@code RendimientoExternoRepository}, el detalle de rendimiento de WhoScored de
     * cada jugador {@code ACTIVO} en la semana en curso y persiste un {@code RendimientoPartido}
     * por cada partido devuelto, actualizando la fecha de última actualización de rendimiento del
     * jugador (UC-07).
     */
    void ingestarRendimiento();
}
