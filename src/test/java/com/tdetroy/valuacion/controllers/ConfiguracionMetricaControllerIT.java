package com.tdetroy.valuacion.controllers;

import static org.springframework.test.web.servlet.assertj.MockMvcTester.create;

import com.tdetroy.valuacion.config.JwtService;
import com.tdetroy.valuacion.model.RolUsuario;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConfiguracionMetricaControllerIT {

    @Autowired private JwtService jwtService;

    private MockMvcTester mvc;

    @Autowired
    void inicializarMvcTester(MockMvc mockMvc) {
        this.mvc = create(mockMvc);
    }

    private String tokenAdmin() {
        return jwtService.generarToken(UUID.randomUUID(), "admin@example.com", RolUsuario.ADMIN);
    }

    private String tokenUser() {
        return jwtService.generarToken(UUID.randomUUID(), "user@example.com", RolUsuario.USER);
    }

    // ---- GET /api/v1/configuracion/metricas ----

    @Test
    void obtenerConfiguracion_sinToken_responde401() {
        mvc.get().uri("/api/v1/configuracion/metricas").assertThat().hasStatus(401);
    }

    @Test
    void obtenerConfiguracion_conTokenUser_responde403() {
        mvc.get()
                .uri("/api/v1/configuracion/metricas")
                .header("Authorization", "Bearer " + tokenUser())
                .assertThat()
                .hasStatus(403);
    }

    @Test
    void obtenerConfiguracion_conTokenAdmin_responde200() {
        mvc.get()
                .uri("/api/v1/configuracion/metricas")
                .header("Authorization", "Bearer " + tokenAdmin())
                .assertThat()
                .hasStatus(200);
    }

    // ---- PUT /api/v1/configuracion/metricas ----

    @Test
    void actualizarConfiguracion_sinToken_responde401() {
        mvc.put()
                .uri("/api/v1/configuracion/metricas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        """
                {
                  "metricas": [
                    {
                      "clave": "goals",
                      "pesoGeneral": 0.5,
                      "activo": true
                    }
                  ]
                }
                """)
                .assertThat()
                .hasStatus(401);
    }

    @Test
    void actualizarConfiguracion_conTokenUser_responde403() {
        mvc.put()
                .uri("/api/v1/configuracion/metricas")
                .header("Authorization", "Bearer " + tokenUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        """
                {
                  "metricas": [
                    {
                      "clave": "goals",
                      "pesoGeneral": 0.5,
                      "activo": true
                    }
                  ]
                }
                """)
                .assertThat()
                .hasStatus(403);
    }

    @Test
    void actualizarConfiguracion_conPesoInvalido_responde400() {
        String json =
                """
                {
                  "metricas": [
                    {
                      "clave": "goals",
                      "pesoGeneral": -0.5,
                      "activo": true,
                      "pesosPorPosicion": {}
                    }
                  ]
                }
                """;

        mvc.put()
                .uri("/api/v1/configuracion/metricas")
                .header("Authorization", "Bearer " + tokenAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .assertThat()
                .hasStatus(400);
    }

    @Test
    void actualizarConfiguracion_conTokenAdminYDatosValidos_responde200YActualiza() {
        String json =
                """
                {
                  "metricas": [
                    {
                      "clave": "goals",
                      "pesoGeneral": 0.5000,
                      "activo": true,
                      "pesosPorPosicion": {
                        "Delantero": 0.5000,
                        "Centrocampista": 0.8000,
                        "Defensor": 1.5000,
                        "Arquero": 3.0000
                      }
                    }
                  ]
                }
                """;

        mvc.put()
                .uri("/api/v1/configuracion/metricas")
                .header("Authorization", "Bearer " + tokenAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .assertThat()
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$[?(@.clave == 'goals')].pesoGeneral")
                .asArray()
                .contains(0.5);
    }
}
