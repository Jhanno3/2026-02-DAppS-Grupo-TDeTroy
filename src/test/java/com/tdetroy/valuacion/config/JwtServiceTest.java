package com.tdetroy.valuacion.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.tdetroy.valuacion.model.RolUsuario;
import com.tdetroy.valuacion.model.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

/**
 * Cubre {@link JwtService}: emisión de un token que embebe {@code usuarioId}/{@code rol}
 * (necesarios para que {@link JwtAuthenticationFilter} reconstruya el principal sin consultar la
 * base) y que la validación rechaza — sin lanzar excepción, ver javadoc del método — cualquier
 * token expirado, firmado con otra clave, o directamente malformado.
 */
class JwtServiceTest {

    private static final String SECRETO = "secreto-de-test-con-al-menos-32-bytes-para-hs256";

    private final JwtService jwtService = new JwtService(SECRETO, 60);

    @Test
    void generarToken_yValidar_devuelveLosMismosClaimsDelUsuario() {
        Usuario usuario = Usuario.registrar("persona@example.com", "hash-bcrypt");

        String token = jwtService.generarToken(usuario);
        Optional<JwtService.ClaimsToken> claims = jwtService.validarYExtraerClaims(token);

        assertThat(claims).isPresent();
        assertThat(claims.get().usuarioId()).isEqualTo(usuario.getId());
        assertThat(claims.get().email()).isEqualTo("persona@example.com");
        assertThat(claims.get().rol()).isEqualTo(RolUsuario.USER);
    }

    @Test
    void validar_tokenExpirado_devuelveVacio() {
        SecretKey key = Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8));
        Instant hace2Horas = Instant.now().minusSeconds(7200);
        String tokenExpirado =
                Jwts.builder()
                        .subject("persona@example.com")
                        .claim("usuarioId", UUID.randomUUID().toString())
                        .claim("rol", "USER")
                        .issuedAt(Date.from(hace2Horas))
                        .expiration(Date.from(hace2Horas.plusSeconds(60)))
                        .signWith(key)
                        .compact();

        assertThat(jwtService.validarYExtraerClaims(tokenExpirado)).isEmpty();
    }

    @Test
    void validar_tokenFirmadoConOtraClave_devuelveVacio() {
        JwtService otroServicio =
                new JwtService("otro-secreto-completamente-distinto-32-bytes+", 60);
        Usuario usuario = Usuario.registrar("persona@example.com", "hash-bcrypt");
        String tokenDeOtraClave = otroServicio.generarToken(usuario);

        assertThat(jwtService.validarYExtraerClaims(tokenDeOtraClave)).isEmpty();
    }

    @Test
    void validar_tokenMalformado_devuelveVacio() {
        assertThat(jwtService.validarYExtraerClaims("esto-no-es-un-jwt")).isEmpty();
    }

    @Test
    void validar_tokenConRolDesconocido_devuelveVacio() {
        SecretKey key = Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8));
        Instant ahora = Instant.now();
        String tokenConRolInvalido =
                Jwts.builder()
                        .subject("persona@example.com")
                        .claim("usuarioId", UUID.randomUUID().toString())
                        .claim("rol", "SUPERADMIN")
                        .issuedAt(Date.from(ahora))
                        .expiration(Date.from(ahora.plusSeconds(3600)))
                        .signWith(key)
                        .compact();

        assertThat(jwtService.validarYExtraerClaims(tokenConRolInvalido)).isEmpty();
    }
}
