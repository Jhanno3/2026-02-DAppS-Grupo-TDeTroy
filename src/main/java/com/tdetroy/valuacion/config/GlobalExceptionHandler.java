package com.tdetroy.valuacion.config;

import com.tdetroy.valuacion.common.exceptions.NegocioException;
import com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Manejador global de errores de la API, en {@code application/problem+json} (RFC 7807), cumpliendo
 * constitution.md §3 ("Errores de API: formato consistente en toda la aplicación... Prohibido
 * devolver stack traces o mensajes de excepción interna crudos al cliente") y el contrato de status
 * codes de plan.md §3.
 *
 * <p>Extiende {@link ResponseEntityExceptionHandler} para heredar, sin reescribirlo, el manejo ya
 * correcto de las excepciones estándar de Spring MVC (validación de forma con {@code @Valid} → 400,
 * JSON malformado → 400, etc. — "400 para validación de forma" de plan.md §3) y sólo agrega los dos
 * casos propios del dominio: violación de una regla de negocio y cualquier error no anticipado. Los
 * errores de autenticación (401) y autorización (403) no pasan por acá: ocurren en el filtro de
 * seguridad, antes de llegar al Controller, y los maneja {@link SecurityConfig} con el mismo
 * formato problem+json (ver {@link ProblemDetailResponseWriter}).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * "409 Conflict para saldo/tenencia/emisión insuficiente" (plan.md §3): toda {@link
     * NegocioException} representa una operación válida en su forma pero rechazada por el estado
     * actual del sistema, nunca por datos de entrada mal formados. El mensaje de la excepción es
     * información de negocio pensada para el usuario final (no un detalle interno de
     * implementación), por eso se expone tal cual en {@code detail}.
     */
    @ExceptionHandler(NegocioException.class)
    public ProblemDetail manejarNegocio(NegocioException ex) {
        ProblemDetail problema =
                ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problema.setTitle("Regla de negocio violada");
        return problema;
    }

    /**
     * "404 para entidad inexistente" (plan.md §3). El mensaje, igual que en {@link
     * #manejarNegocio}, es información pensada para el cliente (qué entidad no existe), no un
     * detalle interno.
     */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail manejarRecursoNoEncontrado(RecursoNoEncontradoException ex) {
        ProblemDetail problema =
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problema.setTitle("Recurso no encontrado");
        return problema;
    }

    /**
     * Red de seguridad final: cualquier excepción no anticipada nunca debe filtrar su mensaje ni su
     * stack trace al cliente (constitution.md §3). Se registra completa en el log del servidor para
     * diagnóstico y se responde un detalle genérico.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail manejarErrorInesperado(Exception ex) {
        log.error("Error no controlado procesando una request", ex);
        ProblemDetail problema =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Ocurrió un error inesperado. Si el problema persiste, contactá al administrador.");
        problema.setTitle("Error interno");
        return problema;
    }
}
