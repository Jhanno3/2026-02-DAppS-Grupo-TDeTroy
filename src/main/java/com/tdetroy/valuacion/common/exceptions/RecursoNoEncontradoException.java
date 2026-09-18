package com.tdetroy.valuacion.common.exceptions;

import java.util.UUID;

/**
 * No existe la entidad solicitada (plan.md §3: "404 para entidad inexistente").
 *
 * <p>Deliberadamente <b>no</b> extiende {@link NegocioException}: esa jerarquía representa una
 * operación válida en su forma pero rechazada por el estado del sistema (409, ej. saldo
 * insuficiente), mientras que esto es un recurso directamente inexistente (404) — son dos contratos
 * HTTP distintos, {@code GlobalExceptionHandler} (config/) los traduce por separado. Genérica sobre
 * {@code tipoEntidad} para que cualquier Service la reutilice (ej. {@code UsuarioServiceImpl},
 * T1.3) en vez de que cada entidad declare su propia excepción "no encontrada" casi idéntica.
 */
public final class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String tipoEntidad, UUID id) {
        super(tipoEntidad + " no encontrado: " + id);
    }
}
