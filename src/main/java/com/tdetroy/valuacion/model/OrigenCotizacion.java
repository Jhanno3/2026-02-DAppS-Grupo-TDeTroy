package com.tdetroy.valuacion.model;

/**
 * Disparador que produjo un {@link CotizacionHistorica} (plan.md §2.4, §6.4): {@code AUTOMATICO}
 * para el job semanal, {@code MANUAL} para el disparo vía {@code POST /cotizaciones/recalculo}
 * (UC-13, restringido a ADMIN). Ambos invocan exactamente el mismo algoritmo de {@code
 * CotizacionService} (constitution.md §2) — este valor sólo documenta el origen del disparo, nunca
 * cambia el cálculo en sí.
 */
public enum OrigenCotizacion {
    AUTOMATICO,
    MANUAL
}
