package com.tdetroy.valuacion.dto.cotizacion;

import com.tdetroy.valuacion.model.cotizacion.EstrategiaCotizacion;

/**
 * Respuesta de {@code PUT /cotizaciones/estrategia}: la estrategia por defecto que quedó vigente.
 */
public record EstrategiaCotizacionResponse(EstrategiaCotizacion estrategia) {}
