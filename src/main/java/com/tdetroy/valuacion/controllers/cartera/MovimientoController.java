package com.tdetroy.valuacion.controllers.cartera;

import com.tdetroy.valuacion.config.seguridad.UsuarioPrincipal;
import com.tdetroy.valuacion.dto.cartera.MovimientoResponse;
import com.tdetroy.valuacion.services.cartera.MovimientoService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Historial de movimientos del usuario autenticado (plan.md §3, UC-12). Siempre el propio usuario —
 * {@code usuarioId} sale del JWT vía {@link UsuarioPrincipal}, nunca de un path variable (spec.md
 * UC-12: "un usuario solo puede ver su propio historial, nunca el de otro usuario").
 */
@RestController
@RequestMapping("/api/v1/movimientos")
public class MovimientoController {

    private final MovimientoService movimientoService;

    public MovimientoController(MovimientoService movimientoService) {
        this.movimientoService = movimientoService;
    }

    @GetMapping("/me")
    public List<MovimientoResponse> obtenerMiHistorial(
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return movimientoService.obtenerHistorial(principal.getUsuarioId()).stream()
                .map(MovimientoResponse::desde)
                .toList();
    }
}
