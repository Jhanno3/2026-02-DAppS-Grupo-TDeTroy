package com.tdetroy.valuacion.controllers;

import static org.springframework.test.web.servlet.assertj.MockMvcTester.create;

import com.tdetroy.valuacion.config.JwtService;
import com.tdetroy.valuacion.model.RolUsuario;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test de integración de {@link CotizacionController} (tasks.md T4.6) contra el contexto Spring
 * completo — mismo criterio que {@code JugadorControllerIT}: prueba de punta a punta que {@code
 * SecurityConfig} ({@code @PreAuthorize("hasRole('ADMIN')")}) y {@code CotizacionService} quedan
 * bien enganchados. Con el catálogo vacío en este contexto de test, {@code recalcular} no tiene
 * ningún jugador {@code ACTIVO} sobre el que iterar — el cálculo en sí ya está cubierto
 * extensamente por {@code CotizacionServiceImplTest}; acá sólo se verifica el cableado HTTP.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CotizacionControllerIT {

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

    @Test
    void recalcular_sinToken_responde401() {
        mvc.post().uri("/api/v1/cotizaciones/recalculo").assertThat().hasStatus(401);
    }

    @Test
    void recalcular_conTokenUser_responde403() {
        mvc.post()
                .uri("/api/v1/cotizaciones/recalculo")
                .header("Authorization", "Bearer " + tokenUser())
                .assertThat()
                .hasStatus(403);
    }

    @Test
    void recalcular_conTokenAdmin_responde202() {
        mvc.post()
                .uri("/api/v1/cotizaciones/recalculo")
                .header("Authorization", "Bearer " + tokenAdmin())
                .assertThat()
                .hasStatus(202);
    }

    @Test
    void recalcular_conTokenAdminYEstrategia_responde202() {
        mvc.post()
                .uri("/api/v1/cotizaciones/recalculo?estrategia=RENDIMIENTO_ACTUAL")
                .header("Authorization", "Bearer " + tokenAdmin())
                .assertThat()
                .hasStatus(202);
    }

    // ---- PUT /cotizaciones/estrategia (ADMIN) ----

    @Test
    void cambiarEstrategia_sinToken_responde401() {
        mvc.put()
                .uri("/api/v1/cotizaciones/estrategia?estrategia=RENDIMIENTO_TEMPORADA")
                .assertThat()
                .hasStatus(401);
    }

    @Test
    void cambiarEstrategia_conTokenUser_responde403() {
        mvc.put()
                .uri("/api/v1/cotizaciones/estrategia?estrategia=RENDIMIENTO_TEMPORADA")
                .header("Authorization", "Bearer " + tokenUser())
                .assertThat()
                .hasStatus(403);
    }

    @Test
    void cambiarEstrategia_conTokenAdmin_responde200ConLaEstrategiaQueQuedoVigente() {
        mvc.put()
                .uri("/api/v1/cotizaciones/estrategia?estrategia=RENDIMIENTO_TEMPORADA")
                .header("Authorization", "Bearer " + tokenAdmin())
                .assertThat()
                .hasStatus(200)
                .bodyText()
                .isEqualTo("{\"estrategia\":\"RENDIMIENTO_TEMPORADA\"}");
    }

    @Test
    void cambiarEstrategia_conValorInexistente_responde400() {
        mvc.put()
                .uri("/api/v1/cotizaciones/estrategia?estrategia=NO_EXISTE")
                .header("Authorization", "Bearer " + tokenAdmin())
                .assertThat()
                .hasStatus(400);
    }

    @Test
    void cambiarEstrategia_sinEstrategia_responde400() {
        mvc.put()
                .uri("/api/v1/cotizaciones/estrategia")
                .header("Authorization", "Bearer " + tokenAdmin())
                .assertThat()
                .hasStatus(400);
    }
}
