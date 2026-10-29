package com.tdetroy.valuacion.model;

/**
 * Sentido con el que una métrica impacta el puntaje de rendimiento (plan.md §6.2/§8.1). {@code
 * PesoMetrica#peso} siempre es positivo (invariante propia), así que el signo del aporte de una
 * métrica al puntaje ponderado viene de acá, no del peso:
 *
 * <ul>
 *   <li>{@link #POSITIVO}: un valor alto puntúa hacia arriba (goles, asistencias, etc.).
 *   <li>{@link #NEGATIVO}: un valor alto puntúa hacia abajo -- para métricas que son un castigo
 *       (tarjetas), donde "más" nunca debería mejorar la cotización del jugador.
 * </ul>
 */
public enum SentidoMetrica {
    POSITIVO,
    NEGATIVO
}
