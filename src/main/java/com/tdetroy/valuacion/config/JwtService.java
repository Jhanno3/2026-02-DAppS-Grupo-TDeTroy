package com.tdetroy.valuacion.config;

import com.tdetroy.valuacion.model.RolUsuario;
import com.tdetroy.valuacion.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Emisión y validación de JWT stateless (plan.md §4, constitution.md §4). Firma HS256 con una clave
 * simétrica de configuración ({@code app.jwt.secret}, nunca commiteada en texto plano — placeholder
 * de desarrollo en {@code application.properties}, sobreescrito por variable de entorno en el resto
 * de los ambientes).
 *
 * <p>El token embebe {@code usuarioId} y {@code rol} como claims propios además del email como
 * {@code subject}, para que {@link JwtAuthenticationFilter} reconstruya el {@link UsuarioPrincipal}
 * completo sin volver a consultar {@code UsuarioRepository} en cada request — la sesión es
 * stateless de punta a punta, no sólo en el servidor no guardando estado, sino en no depender de la
 * base para cada validación.
 */
@Component
public class JwtService {

    private final SecretKey key;
    private final Duration expiracion;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiracion-minutos}") long expiracionMinutos) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiracion = Duration.ofMinutes(expiracionMinutos);
    }

    /** Emite un token para {@code usuario}, válido por {@code app.jwt.expiracion-minutos}. */
    public String generarToken(Usuario usuario) {
        return generarToken(usuario.getId(), usuario.getEmail(), usuario.getRol());
    }

    /**
     * Igual que {@link #generarToken(Usuario)}, a partir de los datos ya resueltos en vez de la
     * entidad completa — lo usa {@code AuthController} en el login (T1.5): el principal que
     * devuelve {@code AuthenticationManager.authenticate(...)} ({@link UsuarioPrincipal}) ya trae
     * {@code usuarioId}/{@code rol}, así que no hace falta una segunda consulta a {@code
     * UsuarioRepository} sólo para poder llamar a este método.
     */
    public String generarToken(UUID usuarioId, String email, RolUsuario rol) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("usuarioId", usuarioId.toString())
                .claim("rol", rol.name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(expiracion)))
                .signWith(key)
                .compact();
    }

    /**
     * Valida la firma y la expiración de {@code token} y extrae sus claims propios.
     *
     * @return vacío si el token es inválido por cualquier motivo (firma incorrecta, expirado,
     *     malformado) — nunca propaga la excepción de jjwt: quien llama (ej. {@link
     *     JwtAuthenticationFilter}) trata un token inválido igual que ausencia de autenticación,
     *     dejando que {@code SecurityConfig} responda 401 de forma consistente con el resto de la
     *     API, en vez de que una excepción no controlada llegue a {@code GlobalExceptionHandler} y
     *     responda 500.
     */
    public Optional<ClaimsToken> validarYExtraerClaims(String token) {
        try {
            Claims claims =
                    Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            UUID usuarioId = UUID.fromString(claims.get("usuarioId", String.class));
            RolUsuario rol = RolUsuario.valueOf(claims.get("rol", String.class));
            return Optional.of(new ClaimsToken(usuarioId, claims.getSubject(), rol));
        } catch (JwtException | IllegalArgumentException | NullPointerException ex) {
            return Optional.empty();
        }
    }

    /** Claims propios ya validados y tipados de un token emitido por {@link #generarToken}. */
    public record ClaimsToken(UUID usuarioId, String email, RolUsuario rol) {}
}
