/**
 * Excepciones de negocio del sistema, en dos familias separadas por el status HTTP al que las
 * traduce {@code GlobalExceptionHandler} (config/): {@link
 * com.tdetroy.valuacion.common.exceptions.NegocioException} (409, regla de negocio violada por el
 * estado del sistema) como raíz abstracta de la primera, y {@link
 * com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException} (404, entidad inexistente)
 * como la segunda, genérica y sin subtipos por entidad.
 *
 * <p>Se lanzan desde {@link com.tdetroy.valuacion.model} (invariantes propias de la entidad) o
 * desde {@link com.tdetroy.valuacion.services}, nunca desde {@code repositories/} ni desde {@code
 * controllers/} (constitution.md §2). El manejador global de errores
 * ({@code @RestControllerAdvice}, config/, T0.4) las traduce a {@code application/problem+json} sin
 * exponer el mensaje interno crudo al cliente (constitution.md §3/§4).
 */
package com.tdetroy.valuacion.common.exceptions;
