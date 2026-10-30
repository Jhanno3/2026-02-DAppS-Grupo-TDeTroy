package com.tdetroy.valuacion.controllers;

import com.tdetroy.valuacion.config.UsuarioPrincipal;
import com.tdetroy.valuacion.dto.request.CrearSubastaRequest;
import com.tdetroy.valuacion.dto.request.PujarSubastaRequest;
import com.tdetroy.valuacion.dto.response.SubastaResponse;
import com.tdetroy.valuacion.model.Subasta;
import com.tdetroy.valuacion.services.SubastaService;
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
 * Subastas de tokens entre usuarios (UC-18). {@code GET} es público (ver {@code
 * config/SecurityConfig}, igual criterio que {@code OfertaController}); {@code POST}/{@code DELETE}
 * requieren autenticación (cualquier rol) y el usuario sale siempre del JWT vía {@link
 * UsuarioPrincipal}, nunca del body ni de la URL.
 */
@RestController
@RequestMapping("/api/v1/subastas")
public class SubastaController {

    private final SubastaService subastaService;

    public SubastaController(SubastaService subastaService) {
        this.subastaService = subastaService;
    }

    /** Subastas activas, de todo el catálogo o sólo de {@code jugadorId}. */
    @GetMapping
    public List<SubastaResponse> listar(@RequestParam(required = false) UUID jugadorId) {
        List<Subasta> subastas =
                jugadorId == null
                        ? subastaService.listarActivas()
                        : subastaService.listarActivasDe(jugadorId);
        return subastas.stream().map(SubastaResponse::desde).toList();
    }

    /** Detalle de una subasta (en cualquier estado) con su historial de pujas. */
    @GetMapping("/{id}")
    public SubastaResponse obtener(@PathVariable UUID id) {
        SubastaService.SubastaDetalle detalle = subastaService.obtenerDetalle(id);
        return SubastaResponse.desde(detalle.subasta(), detalle.pujas());
    }

    @PostMapping
    public ResponseEntity<SubastaResponse> crear(
            @Valid @RequestBody CrearSubastaRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        Subasta subasta =
                subastaService.crear(
                        principal.getUsuarioId(),
                        request.jugadorId(),
                        request.cantidad(),
                        request.precioInicial(),
                        request.duracionMinutos());
        return ResponseEntity.status(HttpStatus.CREATED).body(SubastaResponse.desde(subasta));
    }

    @PostMapping("/{id}/pujas")
    public SubastaResponse pujar(
            @PathVariable UUID id,
            @RequestBody PujarSubastaRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return SubastaResponse.desde(
                subastaService.pujar(principal.getUsuarioId(), id, request.porcentaje()));
    }

    /** El vendedor cancela su subasta activa (en cualquier momento, UC-18). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(
            @PathVariable UUID id, @AuthenticationPrincipal UsuarioPrincipal principal) {
        subastaService.cancelar(principal.getUsuarioId(), id);
        return ResponseEntity.noContent().build();
    }
}
