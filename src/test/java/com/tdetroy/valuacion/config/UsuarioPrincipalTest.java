package com.tdetroy.valuacion.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.tdetroy.valuacion.model.RolUsuario;
import com.tdetroy.valuacion.model.Usuario;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Cubre los dos orígenes de {@link UsuarioPrincipal} (ver javadoc de la clase) y el mapeo de {@link
 * RolUsuario} a {@code GrantedAuthority} con el prefijo {@code ROLE_} que espera {@code
 * hasRole(...)} (plan.md §4).
 */
class UsuarioPrincipalTest {

    @Test
    void desdeUsuario_exponeEmailPasswordHashYAuthorityConPrefijoRole() {
        Usuario usuario = Usuario.registrar("persona@example.com", "hash-bcrypt");

        UsuarioPrincipal principal = UsuarioPrincipal.desde(usuario);

        assertThat(principal.getUsuarioId()).isEqualTo(usuario.getId());
        assertThat(principal.getUsername()).isEqualTo("persona@example.com");
        assertThat(principal.getPassword()).isEqualTo("hash-bcrypt");
        assertThat(principal.getRol()).isEqualTo(RolUsuario.USER);
        assertThat(principal.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_USER");
    }

    @Test
    void desdeClaims_noExponePasswordHashYArmaAuthorityAdmin() {
        UUID usuarioId = UUID.randomUUID();

        UsuarioPrincipal principal =
                UsuarioPrincipal.desde(usuarioId, "admin@example.com", RolUsuario.ADMIN);

        assertThat(principal.getUsuarioId()).isEqualTo(usuarioId);
        assertThat(principal.getUsername()).isEqualTo("admin@example.com");
        assertThat(principal.getPassword()).isNull();
        assertThat(principal.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void cuentaSiempreHabilitadaYNoBloqueada() {
        UsuarioPrincipal principal =
                UsuarioPrincipal.desde(UUID.randomUUID(), "x@example.com", RolUsuario.USER);

        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.isAccountNonExpired()).isTrue();
        assertThat(principal.isAccountNonLocked()).isTrue();
        assertThat(principal.isCredentialsNonExpired()).isTrue();
    }
}
