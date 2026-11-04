package com.tdetroy.valuacion.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tdetroy.valuacion.config.UsuarioPrincipal;
import com.tdetroy.valuacion.dto.request.ActualizarMetricasRequest;
import com.tdetroy.valuacion.dto.response.ConfiguracionMetricaResponse;
import com.tdetroy.valuacion.model.RolUsuario;
import com.tdetroy.valuacion.model.SentidoMetrica;
import com.tdetroy.valuacion.services.ConfiguracionMetricaService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfiguracionMetricaControllerTest {

    @Mock private ConfiguracionMetricaService configuracionMetricaService;

    private ConfiguracionMetricaController controller;

    @BeforeEach
    void setUp() {
        controller = new ConfiguracionMetricaController(configuracionMetricaService);
    }

    @Test
    void obtenerConfiguracion_delegaEnService() {
        ConfiguracionMetricaResponse item =
                new ConfiguracionMetricaResponse(
                        "goals",
                        "Goles",
                        SentidoMetrica.POSITIVO,
                        true,
                        new BigDecimal("0.5"),
                        Map.of());
        when(configuracionMetricaService.obtenerConfiguracion()).thenReturn(List.of(item));

        List<ConfiguracionMetricaResponse> resultado = controller.obtenerConfiguracion();

        assertThat(resultado).containsExactly(item);
        verify(configuracionMetricaService).obtenerConfiguracion();
    }

    @Test
    void actualizarConfiguracion_delegaEnServiceConActorId() {
        UUID actorId = UUID.randomUUID();
        UsuarioPrincipal principal =
                UsuarioPrincipal.desde(actorId, "admin@example.com", RolUsuario.ADMIN);

        ActualizarMetricasRequest request =
                new ActualizarMetricasRequest(
                        List.of(
                                new ActualizarMetricasRequest.ItemActualizarMetrica(
                                        "goals", new BigDecimal("0.5"), true, Map.of())));

        ConfiguracionMetricaResponse item =
                new ConfiguracionMetricaResponse(
                        "goals",
                        "Goles",
                        SentidoMetrica.POSITIVO,
                        true,
                        new BigDecimal("0.5"),
                        Map.of());
        when(configuracionMetricaService.actualizarConfiguracion(request, actorId))
                .thenReturn(List.of(item));

        List<ConfiguracionMetricaResponse> resultado =
                controller.actualizarConfiguracion(request, principal);

        assertThat(resultado).containsExactly(item);
        verify(configuracionMetricaService).actualizarConfiguracion(request, actorId);
    }
}
