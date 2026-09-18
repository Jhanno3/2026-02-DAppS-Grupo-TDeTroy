package com.tdetroy.valuacion.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtro de validación de JWT (tasks.md T1.4, plan.md §4): por cada request, si viene un header
 * {@code Authorization: Bearer <token>} válido, resuelve la autenticación directamente desde los
 * claims del token (sin consultar {@code UsuarioRepository}, ver {@link
 * JwtService#validarYExtraerClaims}) y la deja en el {@code SecurityContext} para el resto de la
 * cadena. Si no hay header, o el token es inválido/expirado, sigue la cadena sin autenticar — es
 * {@link SecurityConfig} (whitelist explícita + {@code authenticationEntryPoint}) quien decide si
 * ese endpoint en particular exige estar autenticado, nunca este filtro.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        extraerToken(request)
                .flatMap(jwtService::validarYExtraerClaims)
                .ifPresent(
                        claims -> {
                            UsuarioPrincipal principal =
                                    UsuarioPrincipal.desde(
                                            claims.usuarioId(), claims.email(), claims.rol());
                            var authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            principal, null, principal.getAuthorities());
                            authentication.setDetails(
                                    new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        });

        filterChain.doFilter(request, response);
    }

    private Optional<String> extraerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(PREFIJO_BEARER)) {
            return Optional.of(header.substring(PREFIJO_BEARER.length()));
        }
        return Optional.empty();
    }
}
