package com.tdetroy.valuacion.config.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autentica a Prometheus con un token fijo, sólo en {@link #RUTA} y sin ningún rol de la app: un
 * JWT no sirve porque vence. Con el token sin configurar no autentica a nadie.
 */
class MetricasTokenFilter extends OncePerRequestFilter {

    static final String RUTA = "/actuator/prometheus";
    static final String AUTORIDAD = "METRICAS";

    private static final RequestMatcher MATCHER =
            PathPatternRequestMatcher.withDefaults().matcher(RUTA);
    private static final String PREFIJO_BEARER = "Bearer ";

    private final String token;

    MetricasTokenFilter(String token) {
        this.token = token == null ? "" : token.trim();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return token.isEmpty() || !MATCHER.matches(request);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && coincide(header)) {
            SecurityContextHolder.getContext()
                    .setAuthentication(
                            new UsernamePasswordAuthenticationToken(
                                    "prometheus",
                                    null,
                                    List.of(new SimpleGrantedAuthority(AUTORIDAD))));
        }
        filterChain.doFilter(request, response);
    }

    /** Comparación en tiempo constante: el tiempo de respuesta no revela el token. */
    private boolean coincide(String header) {
        return MessageDigest.isEqual(
                header.getBytes(StandardCharsets.UTF_8),
                (PREFIJO_BEARER + token).getBytes(StandardCharsets.UTF_8));
    }
}
