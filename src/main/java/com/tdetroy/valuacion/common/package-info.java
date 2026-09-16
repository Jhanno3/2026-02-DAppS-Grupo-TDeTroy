/**
 * Utilidades transversales compartidas entre capas: configuración centralizada de
 * {@link java.math.BigDecimal}/{@link java.math.RoundingMode} para valores monetarios y la
 * jerarquía base de excepciones de negocio ({@code SaldoInsuficienteException},
 * {@code TenenciaInsuficienteException}, {@code EmisionMaximaSuperadaException}, ...).
 *
 * <p>Único punto de extracción cuando dos {@link com.tdetroy.valuacion.services} necesitan
 * compartir lógica, evitando el acoplamiento circular entre Services (constitution.md §2).
 */
package com.tdetroy.valuacion.common;
