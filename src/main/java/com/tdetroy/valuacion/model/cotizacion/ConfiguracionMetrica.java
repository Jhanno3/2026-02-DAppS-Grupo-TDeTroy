package com.tdetroy.valuacion.model.cotizacion;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Configuración vigente de una métrica: peso general y overrides por posición.
 *
 * @param pesosPorPosicion posición -> peso específico
 */
public record ConfiguracionMetrica(
        String clave,
        String nombreLegible,
        SentidoMetrica sentido,
        boolean activo,
        BigDecimal pesoGeneral,
        Map<String, BigDecimal> pesosPorPosicion) {}
