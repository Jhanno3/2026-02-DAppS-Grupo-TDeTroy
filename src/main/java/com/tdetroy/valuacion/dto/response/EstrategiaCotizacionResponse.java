package com.tdetroy.valuacion.dto.response;

import com.tdetroy.valuacion.model.EstrategiaCotizacion;

/**
 * Respuesta de {@code PUT /cotizaciones/estrategia}: la estrategia por defecto que quedó vigente.
 */
public record EstrategiaCotizacionResponse(EstrategiaCotizacion estrategia) {}
