package com.tdetroy.valuacion.controllers.cotizacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tdetroy.valuacion.config.seguridad.UsuarioPrincipal;
import com.tdetroy.valuacion.dto.cotizacion.ActualizarMetricasRequest;
import com.tdetroy.valuacion.dto.cotizacion.ConfiguracionMetricaResponse;
import com.tdetroy.valuacion.model.cotizacion.ConfiguracionMetrica;
import com.tdetroy.valuacion.model.cotizacion.SentidoMetrica;
import com.tdetroy.valuacion.model.usuario.RolUsuario;
import com.tdetroy.valuacion.services.cotizacion.ConfiguracionMetricaService;
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
        ConfiguracionMetrica item =
                new ConfiguracionMetrica(
                        "goals",
                        "Goles",
                        SentidoMetrica.POSITIVO,
                        true,
                        new BigDecimal("0.5"),
                        Map.of());
        when(configuracionMetricaService.obtenerConfiguracion()).thenReturn(List.of(item));

        List<ConfiguracionMetricaResponse> resultado = controller.obtenerConfiguracion();

        assertThat(resultado).containsExactly(ConfiguracionMetricaResponse.desde(item));
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

        ConfiguracionMetrica item =
                new ConfiguracionMetrica(
                        "goals",
                        "Goles",
                        SentidoMetrica.POSITIVO,
                        true,
                        new BigDecimal("0.5"),
                        Map.of());
        when(configuracionMetricaService.actualizarConfiguracion(request.aModelo(), actorId))
                .thenReturn(List.of(item));

        List<ConfiguracionMetricaResponse> resultado =
                controller.actualizarConfiguracion(request, principal);

        assertThat(resultado).containsExactly(ConfiguracionMetricaResponse.desde(item));
        verify(configuracionMetricaService).actualizarConfiguracion(request.aModelo(), actorId);
    }
}
