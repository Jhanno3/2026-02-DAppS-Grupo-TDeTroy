/**
 * Utilidades transversales compartidas entre capas: {@link com.tdetroy.valuacion.common.Monetario}
 * centraliza escala/redondeo de {@link java.math.BigDecimal} para valores monetarios. La
 * jerarquía base de excepciones de negocio vive en el subpaquete
 * {@link com.tdetroy.valuacion.common.exceptions}.
 *
 * <p>Único punto de extracción cuando dos {@link com.tdetroy.valuacion.services} necesitan
 * compartir lógica, evitando el acoplamiento circular entre Services (constitution.md §2).
 */
package com.tdetroy.valuacion.common;
