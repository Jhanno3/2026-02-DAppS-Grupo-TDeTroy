package com.tdetroy.valuacion.model;

/**
 * Estrategias / métodos de cálculo de cotización disponibles (UC-06).
 *
 * <ul>
 *   <li>{@link #RENDIMIENTO_TEMPORADA}: valúa al jugador comparando su rendimiento reciente contra
 *       su propio histórico temporal (normalización min-max de métricas a lo largo de las semanas).
 *       Mide consistencia y evolución respecto a sí mismo.
 *   <li>{@link #RENDIMIENTO_ACTUAL}: valúa al jugador en base a su rendimiento absoluto en la
 *       última semana / partidos recientes (calificación y acciones destacadas inmediatas). Mide
 *       impacto actual y momentum ("mejores jugadores del momento").
 * </ul>
 */
public enum EstrategiaCotizacion {
    RENDIMIENTO_TEMPORADA,
    RENDIMIENTO_ACTUAL
}
