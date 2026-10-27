package com.tdetroy.valuacion.dto.response;

import com.tdetroy.valuacion.model.JugadorConPrecio;
import java.math.BigDecimal;

/**
 * Ítem de {@code GET /jugadores/mercado} (UC-07, plan.md §3bis): extiende {@link JugadorResponse}
 * con la cotización vigente y la variación porcentual, ya calculadas en el Service -- esta pantalla
 * es la única razón por la que esos dos campos existen embebidos en una respuesta de jugador (ver
 * javadoc de {@link JugadorResponse} sobre por qué el catálogo plano no los incluye).
 */
public record JugadorConPrecioResponse(
        JugadorResponse jugador, BigDecimal precio, BigDecimal variacionPorcentual) {

    public static JugadorConPrecioResponse desde(JugadorConPrecio item) {
        return new JugadorConPrecioResponse(
                JugadorResponse.desde(item.jugador()), item.precio(), item.variacionPorcentual());
    }
}
