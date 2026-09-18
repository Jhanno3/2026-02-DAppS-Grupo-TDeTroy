package com.tdetroy.valuacion.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.tdetroy.valuacion.model.Usuario;
import com.tdetroy.valuacion.repositories.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Cubre {@link UsuarioUserDetailsService}: resuelve un {@link UsuarioPrincipal} para un email
 * existente y lanza {@link UsernameNotFoundException} (el contrato que espera {@code
 * AuthenticationManager}, no una excepción propia del dominio) cuando no existe.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioUserDetailsServiceTest {

    @Mock private UsuarioRepository usuarioRepository;

    private UsuarioUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new UsuarioUserDetailsService(usuarioRepository);
    }

    @Test
    void loadUserByUsername_conEmailExistente_devuelveUsuarioPrincipal() {
        Usuario usuario = Usuario.registrar("persona@example.com", "hash-bcrypt");
        when(usuarioRepository.findByEmail("persona@example.com")).thenReturn(Optional.of(usuario));

        UserDetails resultado = service.loadUserByUsername("persona@example.com");

        assertThat(resultado).isInstanceOf(UsuarioPrincipal.class);
        assertThat(resultado.getUsername()).isEqualTo("persona@example.com");
        assertThat(resultado.getPassword()).isEqualTo("hash-bcrypt");
    }

    @Test
    void loadUserByUsername_conEmailInexistente_lanzaUsernameNotFoundException() {
        when(usuarioRepository.findByEmail("fantasma@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("fantasma@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
