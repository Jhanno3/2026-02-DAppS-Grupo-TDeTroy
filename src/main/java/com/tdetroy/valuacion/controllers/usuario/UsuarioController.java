package com.tdetroy.valuacion.controllers.usuario;

import com.tdetroy.valuacion.config.seguridad.UsuarioPrincipal;
import com.tdetroy.valuacion.dto.usuario.RecargarSaldoRequest;
import com.tdetroy.valuacion.dto.usuario.UsuarioResponse;
import com.tdetroy.valuacion.model.usuario.Usuario;
import com.tdetroy.valuacion.services.usuario.UsuarioService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recarga de saldo virtual de un usuario (plan.md §3, UC-14, tasks.md T1.6). Restringido a {@code
 * ADMIN} vía {@link PreAuthorize}, nunca con un chequeo de rol a mano (constitution.md §4). Sin
 * {@code GET}: no está en la whitelist pública de {@code SecurityConfig}, así que por defecto
 * también exige autenticación — un {@code USER} sin rol {@code ADMIN} recibe 403.
 */
@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** UC-14. {@code actorId} sale del JWT ya validado, nunca del body de la request. */
    @PostMapping("/{id}/saldo")
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioResponse recargarSaldo(
            @PathVariable UUID id,
            @Valid @RequestBody RecargarSaldoRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        Usuario usuario =
                usuarioService.recargarSaldo(id, request.monto(), principal.getUsuarioId());
        return UsuarioResponse.desde(usuario);
    }
}
