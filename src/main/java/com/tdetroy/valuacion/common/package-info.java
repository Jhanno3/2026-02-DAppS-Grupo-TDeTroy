/**
 * Utilidades transversales compartidas entre capas: {@link com.tdetroy.valuacion.common.Monetario}
 * centraliza escala/redondeo de {@link java.math.BigDecimal} para valores monetarios, y
 * {@link com.tdetroy.valuacion.common.NegocioException} es la raíz de la jerarquía base de
 * excepciones de negocio ({@link com.tdetroy.valuacion.common.SaldoInsuficienteException},
 * {@link com.tdetroy.valuacion.common.TenenciaInsuficienteException},
 * {@link com.tdetroy.valuacion.common.EmisionMaximaSuperadaException}, ...).
 *
 * <p>Único punto de extracción cuando dos {@link com.tdetroy.valuacion.services} necesitan
 * compartir lógica, evitando el acoplamiento circular entre Services (constitution.md §2).
 */
package com.tdetroy.valuacion.common;
