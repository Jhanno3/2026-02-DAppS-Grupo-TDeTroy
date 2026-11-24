package com.tdetroy.valuacion.services.jugador;

import com.tdetroy.valuacion.repositories.jugador.FilaRankingProjection;
import java.util.List;

/** Estrategia de ordenamiento para el ranking de jugadores (UC-16, plan.md §7). */
public interface EstrategiaRanking {

    /** Identificador único de la estrategia (ej. "cotizacion", "variacion-semanal"). */
    String clave();

    /** Los primeros {@code limite} jugadores, ya ordenados por la base. */
    List<FilaRankingProjection> buscarTop(int limite);
}
