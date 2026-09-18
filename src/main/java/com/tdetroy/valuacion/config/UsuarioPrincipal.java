package com.tdetroy.valuacion.config;

import com.tdetroy.valuacion.model.RolUsuario;
import com.tdetroy.valuacion.model.Usuario;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Principal de Spring Security propio, para que cualquier Controller autenticado (a partir de Fase
 * 5, ej. {@code GET /portfolios/me}) resuelva "quién soy" vía {@code @AuthenticationPrincipal
 * UsuarioPrincipal} sin una consulta extra a {@code UsuarioRepository} — {@link #usuarioId}/{@link
 * #rol} ya vienen embebidos en el JWT (ver {@link JwtService}).
 *
 * <p>Dos orígenes posibles, cada uno con su propio factory: {@link #desde(Usuario)} lo usa {@link
 * UsuarioUserDetailsService} en el momento del login (necesita {@code passwordHash} para que {@code
 * AuthenticationManager} compare credenciales); {@link #desde(UUID, String, RolUsuario)} lo usa
 * {@link JwtAuthenticationFilter} en cada request autenticada posterior, reconstruyendo el
 * principal directamente desde los claims del token — sin volver a tocar la base, que es justamente
 * el punto de que la sesión sea stateless (constitution.md §4).
 */
public class UsuarioPrincipal implements UserDetails {

    private final UUID usuarioId;
    private final String email;
    private final String passwordHash;
    private final RolUsuario rol;

    private UsuarioPrincipal(UUID usuarioId, String email, String passwordHash, RolUsuario rol) {
        validarCampos(usuarioId, email, rol);

        this.usuarioId = usuarioId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.rol = rol;
    }

    private static void validarCampos(UUID usuarioId, String email, RolUsuario rol) {
        Objects.requireNonNull(usuarioId, "usuarioId no puede ser null");
        Objects.requireNonNull(email, "email no puede ser null");
        Objects.requireNonNull(rol, "rol no puede ser null");
    }

    /**
     * A partir de la entidad completa (login: {@code AuthenticationManager} valida el password).
     */
    public static UsuarioPrincipal desde(Usuario usuario) {
        return new UsuarioPrincipal(
                usuario.getId(), usuario.getEmail(), usuario.getPasswordHash(), usuario.getRol());
    }

    /** A partir de los claims ya validados de un JWT (requests posteriores al login). */
    public static UsuarioPrincipal desde(UUID usuarioId, String email, RolUsuario rol) {
        return new UsuarioPrincipal(usuarioId, email, null, rol);
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public RolUsuario getRol() {
        return rol;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
