package com.tdetroy.valuacion.model.jugador;

import java.util.List;

/**
 * Página de Mercado. {@code posicionesDisponibles} refleja el catálogo completo, no el filtro
 * aplicado.
 */
public record PaginaMercado(
        List<JugadorConPrecio> contenido,
        int pagina,
        int tamanioPagina,
        long totalElementos,
        int totalPaginas,
        List<String> posicionesDisponibles) {}
