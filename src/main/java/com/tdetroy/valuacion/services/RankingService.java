package com.tdetroy.valuacion.services;

import com.tdetroy.valuacion.model.JugadorConPrecio;
import java.util.List;

/** Servicio para consulta de rankings de jugadores (UC-16, plan.md §7). */
public interface RankingService {

    /**
     * Obtiene el listado de jugadores ordenado según la estrategia solicitada.
     *
     * @param claveEstrategia clave de la estrategia ("cotizacion", "variacion-semanal" o fallback)
     * @return lista de los jugadores ordenados
     */
    List<JugadorConPrecio> obtenerRanking(String claveEstrategia);
}
