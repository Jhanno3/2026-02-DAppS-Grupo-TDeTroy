package com.tdetroy.valuacion.services.cotizacion;

import com.tdetroy.valuacion.model.cotizacion.AjusteMetrica;
import com.tdetroy.valuacion.model.cotizacion.ConfiguracionMetrica;
import java.util.List;
import java.util.UUID;

/**
 * Consulta y actualización en runtime de la calibración de pesos de métricas y ponderaciones por
 * posición del jugador.
 */
public interface ConfiguracionMetricaService {

    /** Obtiene la lista completa de métricas configuradas, sus sentidos y pesos por posición. */
    List<ConfiguracionMetrica> obtenerConfiguracion();

    /**
     * Actualiza los pesos generales y específicos por posición de las métricas indicadas, dejando
     * traza en auditoría.
     */
    List<ConfiguracionMetrica> actualizarConfiguracion(List<AjusteMetrica> ajustes, UUID actorId);
}
