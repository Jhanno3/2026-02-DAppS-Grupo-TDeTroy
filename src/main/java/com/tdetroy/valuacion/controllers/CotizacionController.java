package com.tdetroy.valuacion.controllers;

import com.tdetroy.valuacion.config.UsuarioPrincipal;
import com.tdetroy.valuacion.model.OrigenCotizacion;
import com.tdetroy.valuacion.services.CotizacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Disparo manual del recalculo de cotizacion (plan.md S3/S6.4, UC-13, tasks.md T4.6). {@code POST
 * /cotizaciones/recalculo} invoca exactamente el mismo {@link CotizacionService#recalcular} que el
 * job semanal (constitution.md S2: nunca una segunda implementacion del algoritmo), sobre todos los
 * jugadores {@code ACTIVO} del catalogo, sin seleccion de un jugador puntual ni ningun campo de
 * tipo cotizacion/valor en el request -- por eso no toma {@code @RequestBody}: el endpoint solo
 * dispara, nunca fija un precio (constitution.md S2).
 *
 * <p>{@code actorId} sale del JWT ya validado (nunca del body) y se propaga a cada {@code
 * RegistroAuditoria} que {@link CotizacionService#calcularCotizacion} escribe por jugador afectado.
 */
@RestController
@RequestMapping("/api/v1/cotizaciones")
public class CotizacionController {

    private final CotizacionService cotizacionService;

    public CotizacionController(CotizacionService cotizacionService) {
        this.cotizacionService = cotizacionService;
    }

    @PostMapping("/recalculo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> recalcular(@AuthenticationPrincipal UsuarioPrincipal principal) {
        cotizacionService.recalcular(OrigenCotizacion.MANUAL, principal.getUsuarioId());
        return ResponseEntity.accepted().build();
    }
}
