package com.tdetroy.valuacion.model.cotizacion;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Cambio pedido sobre la ponderación de una métrica.
 *
 * @param pesosPorPosicion posición -> peso; {@code null} si no cambia ninguna posición
 */
public record AjusteMetrica(
        String clave,
        BigDecimal pesoGeneral,
        boolean activo,
        Map<String, BigDecimal> pesosPorPosicion) {}
