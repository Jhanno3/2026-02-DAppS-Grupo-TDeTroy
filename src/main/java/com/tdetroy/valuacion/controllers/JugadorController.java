package com.tdetroy.valuacion.controllers;

import com.tdetroy.valuacion.config.UsuarioPrincipal;
import com.tdetroy.valuacion.dto.request.CrearJugadorRequest;
import com.tdetroy.valuacion.dto.request.EditarJugadorRequest;
import com.tdetroy.valuacion.dto.response.JugadorResponse;
import com.tdetroy.valuacion.model.Jugador;
import com.tdetroy.valuacion.services.JugadorService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catálogo de jugadores (plan.md §3, UC-03/UC-04/UC-07). {@code GET} es público (UC-07, ver {@code
 * config/SecurityConfig}); {@code POST}/{@code PUT} quedan restringidos a {@code ADMIN} vía {@link
 * PreAuthorize}, nunca con un chequeo de rol a mano (constitution.md §4).
 */
@RestController
@RequestMapping("/api/v1/jugadores")
public class JugadorController {

    private final JugadorService jugadorService;

    public JugadorController(JugadorService jugadorService) {
        this.jugadorService = jugadorService;
    }

    /** UC-07: catálogo completo, incluidos los jugadores dados de baja (spec.md UC-07). */
    @GetMapping
    public List<JugadorResponse> listar() {
        return jugadorService.listar().stream().map(JugadorResponse::desde).toList();
    }

    /** UC-07: detalle de un jugador puntual; 404 vía {@code RecursoNoEncontradoException}. */
    @GetMapping("/{id}")
    public JugadorResponse obtenerPorId(@PathVariable UUID id) {
        return JugadorResponse.desde(jugadorService.obtenerPorId(id));
    }

    /** UC-03. {@code actorId} sale del JWT ya validado, nunca del body de la request. */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JugadorResponse> darAlta(
            @Valid @RequestBody CrearJugadorRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        Jugador jugador =
                jugadorService.darAlta(
                        request.nombre(),
                        request.club(),
                        request.posicion(),
                        request.fechaNacimiento(),
                        request.nacionalidad(),
                        principal.getUsuarioId());
        return ResponseEntity.status(HttpStatus.CREATED).body(JugadorResponse.desde(jugador));
    }

    /** UC-04. Nunca toca cotización ni límite de tokens — eso lo garantiza {@code Jugador}. */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public JugadorResponse editar(
            @PathVariable UUID id,
            @Valid @RequestBody EditarJugadorRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        Jugador jugador =
                jugadorService.editar(
                        id,
                        request.nombre(),
                        request.club(),
                        request.posicion(),
                        request.fechaNacimiento(),
                        request.nacionalidad(),
                        principal.getUsuarioId());
        return JugadorResponse.desde(jugador);
    }
}
