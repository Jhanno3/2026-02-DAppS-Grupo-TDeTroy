package com.tdetroy.valuacion.controllers;

import com.tdetroy.valuacion.config.UsuarioPrincipal;
import com.tdetroy.valuacion.dto.request.ComprarOfertaRequest;
import com.tdetroy.valuacion.dto.request.PublicarOfertaRequest;
import com.tdetroy.valuacion.dto.response.MovimientoResponse;
import com.tdetroy.valuacion.dto.response.OfertaResponse;
import com.tdetroy.valuacion.model.Movimiento;
import com.tdetroy.valuacion.model.OfertaP2P;
import com.tdetroy.valuacion.services.OfertaService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Venta directa entre usuarios mediante oferta abierta (plan.md §3, UC-17). {@code GET} es público
 * (ver {@code config/SecurityConfig}, igual criterio que {@code JugadorController}); {@code POST}/
 * {@code DELETE} requieren autenticación (cualquier rol, sin {@code @PreAuthorize}: no son
 * endpoints administrativos) — {@code usuarioId} sale siempre del JWT ya validado vía {@link
 * UsuarioPrincipal}, nunca del body ni de la URL, así que nadie puede publicar, comprar o cancelar
 * en nombre de otro usuario.
 */
@RestController
@RequestMapping("/api/v1/ofertas")
public class OfertaController {

    private final OfertaService ofertaService;

    public OfertaController(OfertaService ofertaService) {
        this.ofertaService = ofertaService;
    }

    /**
     * UC-17: ofertas abiertas, para que un comprador elija. Sin {@code jugadorId}, devuelve las de
     * todo el catálogo (listado global, "pestaña P2P"); con {@code jugadorId}, sólo las de ese
     * jugador (mismo filtro de antes, usado desde su página de detalle).
     */
    @GetMapping
    public List<OfertaResponse> listar(@RequestParam(required = false) UUID jugadorId) {
        List<OfertaP2P> ofertas =
                jugadorId == null
                        ? ofertaService.listarAbiertas()
                        : ofertaService.listarAbiertasDe(jugadorId);
        return ofertas.stream().map(OfertaResponse::desde).toList();
    }

    /**
     * UC-17: el vendedor publica una cantidad de tokens que posee de un jugador, a su propio
     * precio.
     */
    @PostMapping
    public ResponseEntity<OfertaResponse> publicar(
            @Valid @RequestBody PublicarOfertaRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        OfertaP2P oferta =
                ofertaService.publicar(
                        principal.getUsuarioId(),
                        request.jugadorId(),
                        request.cantidad(),
                        request.precioUnitario(),
                        request.horasLimite());
        return ResponseEntity.status(HttpStatus.CREATED).body(OfertaResponse.desde(oferta));
    }

    /** UC-17: un comprador ejecuta una compra contra la oferta, hasta la cantidad disponible. */
    @PostMapping("/{id}/compras")
    public ResponseEntity<MovimientoResponse> comprar(
            @PathVariable UUID id,
            @Valid @RequestBody ComprarOfertaRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        Movimiento movimiento =
                ofertaService.ejecutar(principal.getUsuarioId(), id, request.cantidad());
        return ResponseEntity.status(HttpStatus.CREATED).body(MovimientoResponse.desde(movimiento));
    }

    /** UC-17: el vendedor cancela lo no vendido de su propia oferta. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(
            @PathVariable UUID id, @AuthenticationPrincipal UsuarioPrincipal principal) {
        ofertaService.cancelar(principal.getUsuarioId(), id);
        return ResponseEntity.noContent().build();
    }
}
