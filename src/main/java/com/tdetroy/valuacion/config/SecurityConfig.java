package com.tdetroy.valuacion.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Esqueleto de {@link SecurityFilterChain} (tasks.md T0.4): postura de seguridad transversal sin
 * ninguna regla de negocio todavía — ni JWT, ni {@code UserDetailsService}, ni
 * {@code @PreAuthorize} por rol. Eso lo completa T1.4 (Fase 1, `Usuario`) sobre esta misma clase,
 * que ya deja resuelto lo estructural:
 *
 * <ul>
 *   <li>Postura por defecto <b>whitelist explícita, no blacklist</b> (plan.md §4): todo requiere
 *       autenticación salvo lo explícitamente listado como público. Hoy sólo la documentación de la
 *       API está en esa lista; cada Controller público (ej. {@code GET /jugadores}) suma su propio
 *       {@code permitAll} cuando ese endpoint se construye, no antes.
 *   <li>Sesión {@link SessionCreationPolicy#STATELESS}: constitution.md §4/plan.md §4 exigen JWT
 *       stateless, nunca sesión de servidor.
 *   <li>CORS por whitelist explícita de orígenes (constitution.md §4), nunca {@code *}.
 *   <li>401/403 responden {@code application/problem+json} igual que el resto de la API (ver {@link
 *       ProblemDetailResponseWriter}), no la página HTML por defecto de Spring Security.
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Rutas de infraestructura (documentación), nunca de negocio, públicas desde el día 1. */
    private static final String[] RUTAS_PUBLICAS_INFRAESTRUCTURA = {
        "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
    };

    private final ProblemDetailResponseWriter problemDetailResponseWriter;

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOriginsCsv;

    public SecurityConfig(ProblemDetailResponseWriter problemDetailResponseWriter) {
        this.problemDetailResponseWriter = problemDetailResponseWriter;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(
                        handling ->
                                handling.authenticationEntryPoint(
                                                (request, response, authException) ->
                                                        problemDetailResponseWriter.escribir(
                                                                response,
                                                                HttpStatus.UNAUTHORIZED,
                                                                "No autenticado",
                                                                "Se requiere un token válido para acceder a este recurso."))
                                        .accessDeniedHandler(
                                                (request, response, accessDeniedException) ->
                                                        problemDetailResponseWriter.escribir(
                                                                response,
                                                                HttpStatus.FORBIDDEN,
                                                                "Acceso denegado",
                                                                "No tenés permisos para acceder a este recurso.")))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(RUTAS_PUBLICAS_INFRAESTRUCTURA)
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated());

        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        List<String> origenesPermitidos =
                Arrays.stream(allowedOriginsCsv.split(","))
                        .map(String::trim)
                        .filter(origen -> !origen.isEmpty())
                        .toList();

        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(origenesPermitidos);
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuracion);
        return source;
    }
}
