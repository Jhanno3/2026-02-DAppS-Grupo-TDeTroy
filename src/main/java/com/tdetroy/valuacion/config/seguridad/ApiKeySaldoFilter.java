package com.tdetroy.valuacion.config.seguridad;

import com.tdetroy.valuacion.config.errores.ProblemDetailResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiKeySaldoFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-API-Key";
    static final String RUTA_SALDO = "/api/v1/usuarios/*/saldo";

    private static final RequestMatcher MATCHER =
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, RUTA_SALDO);

    private final String apiKeyEsperada;
    private final ProblemDetailResponseWriter problemDetailResponseWriter;

    public ApiKeySaldoFilter(
            String apiKeyEsperada, ProblemDetailResponseWriter problemDetailResponseWriter) {
        this.apiKeyEsperada = apiKeyEsperada == null ? "" : apiKeyEsperada.trim();
        this.problemDetailResponseWriter = problemDetailResponseWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !MATCHER.matches(request);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String apiKeyRecibida = request.getHeader(HEADER);
        if (apiKeyEsperada.isEmpty() || apiKeyRecibida == null || !coincide(apiKeyRecibida)) {
            problemDetailResponseWriter.escribir(
                    response,
                    HttpStatus.UNAUTHORIZED,
                    "API Key inválida",
                    "Se requiere una API Key válida para operar con carga de saldo.");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean coincide(String apiKeyRecibida) {
        return MessageDigest.isEqual(
                apiKeyRecibida.trim().getBytes(StandardCharsets.UTF_8),
                apiKeyEsperada.getBytes(StandardCharsets.UTF_8));
    }
}
