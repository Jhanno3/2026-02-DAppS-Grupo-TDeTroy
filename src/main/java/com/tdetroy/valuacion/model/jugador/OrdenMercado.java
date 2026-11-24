package com.tdetroy.valuacion.model.jugador;

/** Orden del catálogo de Mercado; por defecto {@link #PRECIO_DESC}, a pedido de producto. */
public enum OrdenMercado {
    NOMBRE,
    PRECIO_DESC,
    PRECIO_ASC,
    SUBA_DESC,
    DISPONIBLES_DESC;

    /** Un valor ausente o no reconocido cae en {@link #PRECIO_DESC}, sin rechazar la request. */
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
