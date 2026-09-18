/**
 * Jerarquía base de excepciones de negocio, con {@link
 * com.tdetroy.valuacion.common.exceptions.NegocioException} como raíz abstracta.
 *
 * <p>Se lanzan desde {@link com.tdetroy.valuacion.model} (invariantes propias de la entidad) o
 * desde {@link com.tdetroy.valuacion.services}, nunca desde {@code repositories/} ni desde {@code
 * controllers/} (constitution.md §2). El manejador global de errores
 * ({@code @RestControllerAdvice}, config/, T0.4) las traduce a {@code application/problem+json} sin
 * exponer el mensaje interno crudo al cliente (constitution.md §3/§4).
 */
package com.tdetroy.valuacion.common.exceptions;
