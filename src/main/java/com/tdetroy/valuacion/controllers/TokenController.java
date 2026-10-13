package com.tdetroy.valuacion.controllers;

import com.tdetroy.valuacion.config.UsuarioPrincipal;
import com.tdetroy.valuacion.dto.request.ComprarTokensRequest;
import com.tdetroy.valuacion.dto.request.VenderTokensRequest;
import com.tdetroy.valuacion.dto.response.MovimientoResponse;
import com.tdetroy.valuacion.model.Movimiento;
import com.tdetroy.valuacion.services.TokenService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Compra y venta de tokens de un jugador contra el sistema (plan.md §3, UC-09/UC-10). Requiere
 * autenticación (cualquier rol, sin {@code @PreAuthorize}: no son endpoints administrativos) —
 * {@code usuarioId} sale siempre del JWT ya validado vía {@link UsuarioPrincipal}, nunca del body
 * ni de la URL, así que nadie puede operar en nombre de otro usuario.
 */
@RestController
@RequestMapping("/api/v1/jugadores")
public class TokenController {

    private final TokenService tokenService;

    public TokenController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    /** UC-09. */
    @PostMapping("/{id}/compras")
    public ResponseEntity<MovimientoResponse> comprar(
            @PathVariable UUID id,
            @Valid @RequestBody ComprarTokensRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        Movimiento movimiento =
                tokenService.comprar(principal.getUsuarioId(), id, request.cantidad());
        return ResponseEntity.status(HttpStatus.CREATED).body(MovimientoResponse.desde(movimiento));
    }

    /** UC-10. */
    @PostMapping("/{id}/ventas")
    public ResponseEntity<MovimientoResponse> vender(
            @PathVariable UUID id,
            @Valid @RequestBody VenderTokensRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        Movimiento movimiento =
                tokenService.vender(principal.getUsuarioId(), id, request.cantidad());
        return ResponseEntity.status(HttpStatus.CREATED).body(MovimientoResponse.desde(movimiento));
    }
}
