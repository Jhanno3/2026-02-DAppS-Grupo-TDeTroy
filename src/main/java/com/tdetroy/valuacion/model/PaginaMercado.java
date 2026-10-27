package com.tdetroy.valuacion.model;

import java.util.List;

/**
 * Una página del catálogo de Mercado ya filtrado, ordenado y paginado (UC-07, plan.md §3bis).
 * {@code posicionesDisponibles} siempre refleja el catálogo completo (nunca el filtro aplicado en
 * esta consulta puntual), para que el frontend pueda armar sus pills de filtro sin que desaparezcan
 * al filtrar por posición o buscar.
 */
public record PaginaMercado(
        List<JugadorConPrecio> contenido,
        int pagina,
        int tamanioPagina,
        long totalElementos,
        int totalPaginas,
        List<String> posicionesDisponibles) {}
