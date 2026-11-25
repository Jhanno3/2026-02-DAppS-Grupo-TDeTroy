package com.tdetroy.valuacion.config.trazabilidad;

import com.tdetroy.valuacion.common.CorrelationId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Corre antes que la seguridad, para cubrir también las requests rechazadas. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String id = CorrelationId.aceptarOGenerar(request.getHeader(CorrelationId.HEADER));
        response.setHeader(CorrelationId.HEADER, id);
        try (MDC.MDCCloseable ignorado = MDC.putCloseable(CorrelationId.CLAVE_MDC, id)) {
            filterChain.doFilter(request, response);
        }
    }
}
