package com.tdetroy.valuacion.model.rendimiento;

/**
 * Fuente de un {@link RendimientoPartido}.
 *
 * <ul>
 *   <li>{@link #FOOTBALL_DATA}: fixture confirmado, con {@code metricas} vacío.
 *   <li>{@link #WHOSCORED_LIGA}: delta semanal de las métricas acumuladas de temporada.
 * </ul>
 */
public enum FuenteResultado {
    FOOTBALL_DATA,
    WHOSCORED_LIGA
}
