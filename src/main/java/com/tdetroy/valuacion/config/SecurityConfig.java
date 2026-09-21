package com.tdetroy.valuacion.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * {@link SecurityFilterChain} (tasks.md T0.4 + T1.4): postura de seguridad transversal, completada
 * en T1.4 (Fase 1, `Usuario`) con JWT stateless, {@code UserDetailsService} y BCrypt sobre el
 * esqueleto que dejó T0.4:
 *
 * <ul>
 *   <li>Postura por defecto <b>whitelist explícita, no blacklist</b> (plan.md §4): todo requiere
 *       autenticación salvo lo explícitamente listado como público. Hoy sólo la documentación de la
 *       API está en esa lista; cada Controller público (ej. {@code GET /jugadores}) suma su propio
 *       {@code permitAll} cuando ese endpoint se construye, no antes — {@code /auth/**} lo suma
 *       {@code AuthController} en T1.5.
 *   <li>Sesión {@link SessionCreationPolicy#STATELESS}: constitution.md §4/plan.md §4 exigen JWT
 *       stateless, nunca sesión de servidor. {@link JwtAuthenticationFilter} resuelve la
 *       autenticación de cada request antes del filtro estándar de usuario/password de Spring
 *       Security (que este esqueleto ni usa, {@code formLogin}/{@code httpBasic} están
 *       deshabilitados — el único punto de entrada de credenciales es {@code POST /auth/login}).
 *   <li>{@link #passwordEncoder()} (BCrypt, constitution.md §4) y el {@code UserDetailsService}
 *       ({@link UsuarioUserDetailsService}) los detecta Spring Boot automáticamente para armar el
 *       {@code DaoAuthenticationProvider} que expone {@link #authenticationManager}, consumido por
 *       {@code AuthController} (T1.5) para validar credenciales en el login.
 *   <li>{@link EnableMethodSecurity} habilita {@code @PreAuthorize("hasRole('ADMIN')")} sobre
 *       métodos de Controller (plan.md §4) — sin esto la anotación se ignora en silencio. {@link
 *       UsuarioPrincipal#getAuthorities()} expone el rol como {@code GrantedAuthority} con el
 *       prefijo {@code ROLE_} que esa expresión espera.
 *   <li>CORS por whitelist explícita de orígenes (constitution.md §4), nunca {@code *}.
 *   <li>401/403 responden {@code application/problem+json} igual que el resto de la API (ver {@link
 *       ProblemDetailResponseWriter}), no la página HTML por defecto de Spring Security.
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** Rutas de infraestructura (documentación), nunca de negocio, públicas desde el día 1. */
    private static final String[] RUTAS_PUBLICAS_INFRAESTRUCTURA = {
        "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
    };

    /**
     * {@code AuthController} (T1.5, UC-01/UC-02): sin esto, un visitante sin cuenta no podría
     * registrarse ni loguearse — la whitelist es explícita (plan.md §4), así que cada Controller
     * público suma su propia entrada acá cuando se construye, nunca antes.
     */
    private static final String[] RUTAS_PUBLICAS_AUTH = {
        "/api/v1/auth/registro", "/api/v1/auth/login"
    };

    /**
     * {@code JugadorController} (T2.5, UC-07): todo {@code GET} bajo {@code /api/v1/jugadores/**}
     * es público, incluido el propio {@code /api/v1/jugadores} — esto también cubre de antemano las
     * lecturas públicas que sumarán T4.7 ({@code GET .../cotizaciones}) y T7.4 ({@code GET
     * .../ranking}), todas bajo el mismo prefijo y públicas según plan.md §3. {@code POST}/{@code
     * PUT} quedan fuera de esta whitelist a propósito: los protege {@code @PreAuthorize} en el
     * propio Controller, restringido por método HTTP acá para no abrirlos por error.
     */
    private static final String[] RUTAS_PUBLICAS_JUGADORES_GET = {
        "/api/v1/jugadores", "/api/v1/jugadores/**"
    };

    private final ProblemDetailResponseWriter problemDetailResponseWriter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOriginsCsv;

    public SecurityConfig(
            ProblemDetailResponseWriter problemDetailResponseWriter,
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.problemDetailResponseWriter = problemDetailResponseWriter;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /** BCrypt (constitution.md §4: "Prohibido almacenar contraseñas en texto plano..."). */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Expone el {@link AuthenticationManager} que arma Spring Boot a partir del único {@code
     * UserDetailsService} ({@link UsuarioUserDetailsService}) y el único {@link PasswordEncoder}
     * ({@link #passwordEncoder()}) del contexto (vía {@link DaoAuthenticationProvider}), para que
     * {@code AuthController} (T1.5) valide credenciales en {@code POST /auth/login} sin
     * reimplementar esa comparación a mano.
     */
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
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
                                        .requestMatchers(RUTAS_PUBLICAS_AUTH)
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.GET, RUTAS_PUBLICAS_JUGADORES_GET)
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .addFilterBefore(
                        jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

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
