package com.tdetroy.valuacion.config;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Escribe una respuesta {@code application/problem+json} (RFC 7807) directamente sobre un
 * {@link HttpServletResponse}, para los dos puntos de error que ocurren en el filtro de
 * seguridad (401/403, {@link SecurityConfig}) — antes de que la request llegue al
 * {@code DispatcherServlet}, así que no pasan por {@link GlobalExceptionHandler}
 * (constitution.md §3: mismo formato de error en toda la aplicación, sin excepción).
 *
 * <p>Usa el mismo {@link ObjectMapper} (Jackson 3, paquete {@code tools.jackson}) que
 * autoconfigura {@code spring-boot-starter-jackson} para la conversión HTTP del resto de la
 * API — Spring Boot 4 cambió la implementación por defecto de Jackson 2 a Jackson 3.
 */
@Component
class ProblemDetailResponseWriter {

    private final ObjectMapper objectMapper;

    ProblemDetailResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    void escribir(HttpServletResponse response, HttpStatus status, String titulo, String detalle) throws IOException {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalle);
        problema.setTitle(titulo);

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), problema);
    }
}
