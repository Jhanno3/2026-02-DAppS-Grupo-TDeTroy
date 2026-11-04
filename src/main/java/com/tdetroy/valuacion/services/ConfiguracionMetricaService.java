package com.tdetroy.valuacion.services;

import com.tdetroy.valuacion.dto.request.ActualizarMetricasRequest;
import com.tdetroy.valuacion.dto.response.ConfiguracionMetricaResponse;
import java.util.List;
import java.util.UUID;

/**
 * Consulta y actualización en runtime de la calibración de pesos de métricas y ponderaciones por
 * posición del jugador.
 */
public interface ConfiguracionMetricaService {

    /** Obtiene la lista completa de métricas configuradas, sus sentidos y pesos por posición. */
    List<ConfiguracionMetricaResponse> obtenerConfiguracion();

    /**
     * Actualiza los pesos generales y específicos por posición de las métricas indicadas, dejando
     * traza en auditoría.
     */
    List<ConfiguracionMetricaResponse> actualizarConfiguracion(
            ActualizarMetricasRequest request, UUID actorId);
}
