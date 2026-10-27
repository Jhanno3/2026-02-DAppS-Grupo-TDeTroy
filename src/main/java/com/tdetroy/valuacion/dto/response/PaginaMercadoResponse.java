package com.tdetroy.valuacion.dto.response;

import com.tdetroy.valuacion.model.PaginaMercado;
import java.util.List;

/**
 * Respuesta de {@code GET /jugadores/mercado} (UC-07, plan.md §3bis): una página ya filtrada,
 * ordenada y paginada del catálogo, con la cotización de cada jugador embebida.
 */
public record PaginaMercadoResponse(
        List<JugadorConPrecioResponse> contenido,
        int pagina,
        int tamanioPagina,
        long totalElementos,
        int totalPaginas,
        List<String> posicionesDisponibles) {

    public static PaginaMercadoResponse desde(PaginaMercado pagina) {
        return new PaginaMercadoResponse(
                pagina.contenido().stream().map(JugadorConPrecioResponse::desde).toList(),
                pagina.pagina(),
                pagina.tamanioPagina(),
                pagina.totalElementos(),
                pagina.totalPaginas(),
                pagina.posicionesDisponibles());
    }
}
