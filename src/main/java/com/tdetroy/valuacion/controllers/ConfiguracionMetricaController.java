package com.tdetroy.valuacion.controllers;

import com.tdetroy.valuacion.config.UsuarioPrincipal;
import com.tdetroy.valuacion.dto.request.ActualizarMetricasRequest;
import com.tdetroy.valuacion.dto.response.ConfiguracionMetricaResponse;
import com.tdetroy.valuacion.services.ConfiguracionMetricaService;
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
        return configuracionMetricaService.obtenerConfiguracion();
    }

    /** Actualiza la ponderación de las métricas indicadas y audita el cambio. */
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ConfiguracionMetricaResponse> actualizarConfiguracion(
            @Valid @RequestBody ActualizarMetricasRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return configuracionMetricaService.actualizarConfiguracion(
                request, principal.getUsuarioId());
    }
}
