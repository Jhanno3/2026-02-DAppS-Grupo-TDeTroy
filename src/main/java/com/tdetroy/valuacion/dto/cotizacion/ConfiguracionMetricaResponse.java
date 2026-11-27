package com.tdetroy.valuacion.dto.cotizacion;

import com.tdetroy.valuacion.model.cotizacion.ConfiguracionMetrica;
import com.tdetroy.valuacion.model.cotizacion.SentidoMetrica;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Representación de una métrica con su configuración de ponderación general y por posición.
 *
 * @param clave identificador técnico de la métrica (ej. "goals", "assists")
 * @param nombreLegible etiqueta legible para el usuario (ej. "Goles", "Asistencias")
 * @param sentido POSITIVO (suma) o NEGATIVO (resta)
 * @param activo si participa actualmente del cálculo de cotización
 * @param pesoGeneral peso base por defecto
 * @param pesosPorPosicion mapa de posición -> peso específico (ej. "Delantero" -> 0.50, "Defensor"
 *     -> 1.50)
 */
public record ConfiguracionMetricaResponse(
        String clave,
        String nombreLegible,
        SentidoMetrica sentido,
        boolean activo,
        BigDecimal pesoGeneral,
        Map<String, BigDecimal> pesosPorPosicion) {

    public static ConfiguracionMetricaResponse desde(ConfiguracionMetrica configuracion) {
        return new ConfiguracionMetricaResponse(
                configuracion.clave(),
                configuracion.nombreLegible(),
                configuracion.sentido(),
                configuracion.activo(),
                configuracion.pesoGeneral(),
                configuracion.pesosPorPosicion());
    }
}
