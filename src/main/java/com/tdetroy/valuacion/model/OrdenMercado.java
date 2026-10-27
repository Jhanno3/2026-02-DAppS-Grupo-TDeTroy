package com.tdetroy.valuacion.model;

/**
 * Criterio de orden del catálogo paginado de Mercado (UC-07, plan.md §3bis). El valor por defecto
 * es {@link #PRECIO_DESC} — no {@code nombre} — porque lo relevante al navegar el mercado es la
 * cotización, no el orden alfabético (pedido explícito de producto).
 */
public enum OrdenMercado {
    NOMBRE,
    PRECIO_DESC,
    PRECIO_ASC,
    SUBA_DESC,
    DISPONIBLES_DESC;

    /**
     * Traduce el parámetro de query (tal como lo manda el frontend, ej. {@code "precio-desc"}) al
     * enum. Cualquier valor ausente o no reconocido cae en {@link #PRECIO_DESC} — nunca rechaza la
     * request con un 400 por un query param de orden inválido, mismo criterio de tolerancia que el
     * resto de los filtros opcionales de este endpoint (posición/búsqueda).
     */
    public static OrdenMercado desde(String valor) {
        if (valor == null) {
            return PRECIO_DESC;
        }
        return switch (valor.trim().toLowerCase()) {
            case "nombre" -> NOMBRE;
            case "precio-asc" -> PRECIO_ASC;
            case "suba" -> SUBA_DESC;
            case "disponibles" -> DISPONIBLES_DESC;
            case "precio-desc" -> PRECIO_DESC;
            default -> PRECIO_DESC;
        };
    }
}
