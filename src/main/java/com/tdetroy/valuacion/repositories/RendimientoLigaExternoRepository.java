package com.tdetroy.valuacion.repositories;

import java.util.List;

/**
 * Puerto hacia el acumulado de temporada por liga de WhoScored (plan.md §8.1) — reemplaza, para las
 * métricas de rendimiento en sí, al scraping por jugador que antes exponía {@code
 * RendimientoExternoRepository#obtenerRendimiento} (eliminado): una sola página por liga trae a
 * todos los jugadores de esa liga, en vez de una request por jugador. {@code
 * RendimientoServiceImpl} nunca conoce el formato de respuesta de WhoScored ni su mecanismo de
 * paginación, sólo esta interfaz y {@link JugadorRendimientoLigaCrudo}.
 *
 * <p>Implementada por {@link WhoScoredLigaRepositoryImpl}.
 */
public interface RendimientoLigaExternoRepository {

    /**
     * Acumulado de temporada de todos los jugadores listados en la página pública de estadísticas
     * de {@code liga} (filtro "All players" aplicado, recorriendo todas las páginas de a 10
     * resultados hasta agotarlas). Lista vacía si {@code liga} no tiene URL configurada (plan.md
     * §8.1: config liga→URL aparte) o si la fuente no está disponible — nunca lanza, mismo criterio
     * de aislamiento de errores que el resto de los repositories de fuentes externas
     * (constitution.md §1).
     */
    List<JugadorRendimientoLigaCrudo> obtenerAcumuladoLiga(String liga);
}
