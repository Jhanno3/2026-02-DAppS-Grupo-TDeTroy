package com.tdetroy.valuacion.common.exceptions;

/**
 * Raíz de toda excepción de regla de negocio del sistema.
 *
 * <p>Se lanza desde {@link com.tdetroy.valuacion.model} (invariantes propias de la entidad,
 * constitution.md §2) o desde {@link com.tdetroy.valuacion.services}, nunca desde {@link
 * com.tdetroy.valuacion.repositories} ni desde {@code controllers/}. El manejador global de errores
 * ({@code @RestControllerAdvice}, config/, T0.4) la traduce a {@code application/problem+json} sin
 * exponer el mensaje interno crudo al cliente (constitution.md §3/§4).
 *
 * <p>Es {@code abstract}: todo punto de lanzamiento usa un subtipo concreto y nombrado (ej. {@link
 * SaldoInsuficienteException}) — nunca esta clase directamente.
 */
public abstract class NegocioException extends RuntimeException {

    protected NegocioException(String message) {
        super(message);
    }
}
