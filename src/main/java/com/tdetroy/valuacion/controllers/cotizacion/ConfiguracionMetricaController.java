package com.tdetroy.valuacion.controllers.cotizacion;

import com.tdetroy.valuacion.config.seguridad.UsuarioPrincipal;
import com.tdetroy.valuacion.dto.cotizacion.ActualizarMetricasRequest;
import com.tdetroy.valuacion.dto.cotizacion.ConfiguracionMetricaResponse;
import com.tdetroy.valuacion.services.cotizacion.ConfiguracionMetricaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller para consulta y ajuste en runtime de las ponderaciones de métricas (ADMIN). */
@RestController
@RequestMapping("/api/v1/configuracion/metricas")
public class ConfiguracionMetricaController {

    private final ConfiguracionMetricaService configuracionMetricaService;

    public ConfiguracionMetricaController(ConfiguracionMetricaService configuracionMetricaService) {
        this.configuracionMetricaService = configuracionMetricaService;
    }

    /** Lista todas las métricas y sus pesos configurados (general y por posición). */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ConfiguracionMetricaResponse> obtenerConfiguracion() {
        return configuracionMetricaService.obtenerConfiguracion().stream()
                .map(ConfiguracionMetricaResponse::desde)
                .toList();
    }

    /** Actualiza la ponderación de las métricas indicadas y audita el cambio. */
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ConfiguracionMetricaResponse> actualizarConfiguracion(
            @Valid @RequestBody ActualizarMetricasRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return configuracionMetricaService
                .actualizarConfiguracion(request.aModelo(), principal.getUsuarioId())
                .stream()
                .map(ConfiguracionMetricaResponse::desde)
                .toList();
    }
}
