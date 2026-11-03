package com.tdetroy.valuacion.services;

/** Estrategia de ordenamiento para el ranking de jugadores (UC-16, plan.md §7). */
public interface EstrategiaRanking {

    /** Identificador único de la estrategia (ej. "cotizacion", "variacion-semanal"). */
    String clave();

    /** Clave de ordenamiento correspondiente en el motor de catálogo/mercado. */
    String ordenMercado();
}
