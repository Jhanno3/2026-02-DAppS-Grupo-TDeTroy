package com.tdetroy.valuacion.config.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

class ApiKeyRecalculoFilter extends OncePerRequestFilter {

    static final String RUTA = "/api/v1/cotizaciones/recalculo";
    static final String HEADER = "X-API-Key";
    static final String AUTORIDAD = "RECALCULO_API_KEY";

    private static final RequestMatcher MATCHER =
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, RUTA);

    private final String apiKey;

    ApiKeyRecalculoFilter(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return apiKey.isEmpty() || !MATCHER.matches(request);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        if (header != null && coincide(header)) {
            SecurityContextHolder.getContext()
                    .setAuthentication(
                            new UsernamePasswordAuthenticationToken(
                                    "api-key-recalculo",
                                    null,
                                    List.of(new SimpleGrantedAuthority(AUTORIDAD))));
        }
        filterChain.doFilter(request, response);
    }

    private boolean coincide(String header) {
        return MessageDigest.isEqual(
                header.trim().getBytes(StandardCharsets.UTF_8),
                apiKey.getBytes(StandardCharsets.UTF_8));
    }
}
