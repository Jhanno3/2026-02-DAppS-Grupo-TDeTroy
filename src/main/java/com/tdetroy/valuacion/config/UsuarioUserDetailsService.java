package com.tdetroy.valuacion.config;

import com.tdetroy.valuacion.model.Usuario;
import com.tdetroy.valuacion.repositories.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * {@link UserDetailsService} de Spring Security sobre {@link UsuarioRepository} (tasks.md T1.4,
 * plan.md §4). Se nombra distinto de {@code UsuarioService} (services/) a propósito: no es la capa
 * de negocio de {@link Usuario} (esa vive en services/, UC-01/UC-14), es el punto de integración
 * específico que {@code AuthenticationManager} usa para resolver credenciales en el momento del
 * login (UC-02) — por eso vive en {@code config/} y consume el Repository directamente, no a través
 * de un Service (constitution.md §2: la prohibición de acoplamiento inverso es entre {@code
 * model/repositories/services/controllers}, no sobre infraestructura de Spring Security).
 */
@Component
public class UsuarioUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario =
                usuarioRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () ->
                                        new UsernameNotFoundException(
                                                "No existe una cuenta con el email " + email));
        return UsuarioPrincipal.desde(usuario);
    }
}
