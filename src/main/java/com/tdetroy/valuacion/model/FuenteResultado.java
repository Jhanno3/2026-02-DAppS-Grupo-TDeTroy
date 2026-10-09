package com.tdetroy.valuacion.model;

/**
 * Fuente que aportó el resultado/alineación/fixture de un {@link RendimientoPartido} (plan.md
 * §2.3). Sólo Football-Data.org resuelve esa parte de la ingesta (tasks.md T3.4); WhoScored
 * (tasks.md T3.3) aporta las métricas de rendimiento en sí ({@code metricas}), no esta marca de
 * fuente.
 */
public enum FuenteResultado {
    FOOTBALL_DATA
}
