package com.tdetroy.valuacion.config.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import com.tdetroy.valuacion.model.usuario.RolUsuario;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Exposición y permisos de los endpoints de Actuator. */
@SpringBootTest(
        properties = {
            "app.metricas.token=token-de-prueba",
            "management.prometheus.metrics.export.enabled=true"
        })
@AutoConfigureMockMvc
class ActuatorIT {

    @Autowired private JwtService jwtService;
    private MockMvcTester mvc;

    @Autowired
    void inicializarMvcTester(MockMvc mockMvc) {
        this.mvc = MockMvcTester.create(mockMvc);
    }

    private MvcTestResult consultar(String ruta) {
        return mvc.get().uri("/actuator" + ruta).exchange();
    }

    private MvcTestResult consultarComo(RolUsuario rol, String ruta) {
        String token = jwtService.generarToken(UUID.randomUUID(), "actuator@example.com", rol);
        return consultarConToken(token, ruta);
    }

    private MvcTestResult consultarConToken(String token, String ruta) {
        return mvc.get()
                .uri("/actuator" + ruta)
                .header("Authorization", "Bearer " + token)
                .exchange();
    }

    @Test
    void health_sinAutenticacion_muestraSoloElEstado() {
        MvcTestResult resultado = consultar("/health");

        assertThat(resultado).hasStatusOk();
        assertThat(resultado).bodyJson().extractingPath("$.status").isEqualTo("UP");
        assertThat(resultado).bodyJson().doesNotHavePath("$.components");
    }

    @Test
    void health_comoUser_noMuestraElDetalle() {
        MvcTestResult resultado = consultarComo(RolUsuario.USER, "/health");

        assertThat(resultado).hasStatusOk();
        assertThat(resultado).bodyJson().doesNotHavePath("$.components");
    }

    @Test
    void health_comoAdmin_muestraElEstadoDeLaBase() {
        MvcTestResult resultado = consultarComo(RolUsuario.ADMIN, "/health");

        assertThat(resultado).hasStatusOk();
        assertThat(resultado).bodyJson().extractingPath("$.components.db.status").isEqualTo("UP");
    }

    @Test
    void info_sinAutenticacion_muestraElNombreDeLaApp() {
        MvcTestResult resultado = consultar("/info");

        assertThat(resultado).hasStatusOk();
        assertThat(resultado)
                .bodyJson()
                .extractingPath("$.app.name")
                .isEqualTo("Valuacion de jugadores");
    }

    @Test
    void metrics_requiereAdmin() {
        assertThat(consultar("/metrics")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(consultarComo(RolUsuario.USER, "/metrics")).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(consultarComo(RolUsuario.ADMIN, "/metrics")).hasStatusOk();
    }

    @Test
    void metrics_incluyeLaLatenciaDeLasRequests() {
        consultar("/health");

        MvcTestResult resultado = consultarComo(RolUsuario.ADMIN, "/metrics/http.server.requests");

        assertThat(resultado).hasStatusOk();
        assertThat(resultado).bodyJson().extractingPath("$.name").isEqualTo("http.server.requests");
    }

    @Test
    void prometheus_conElTokenFijoOComoAdmin_exponeElHistogramaDeLatencia() {
        consultar("/health");

        assertThat(consultarConToken("token-de-prueba", "/prometheus"))
                .hasStatusOk()
                .bodyText()
                .contains("http_server_requests_seconds_bucket");
        assertThat(consultarComo(RolUsuario.ADMIN, "/prometheus")).hasStatusOk();
    }

    @Test
    void prometheus_sinElToken_rechaza() {
        assertThat(consultar("/prometheus")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(consultarConToken("otro", "/prometheus")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(consultarComo(RolUsuario.USER, "/prometheus")).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void elTokenDePrometheus_noSirveParaOtrasRutas() {
        assertThat(consultarConToken("token-de-prueba", "/metrics"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(
                        mvc.get()
                                .uri("/api/v1/portfolios/me")
                                .header("Authorization", "Bearer token-de-prueba")
                                .exchange())
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void env_noEstaExpuestoNiParaAdmin() {
        assertThat(consultarComo(RolUsuario.ADMIN, "/env")).hasStatus(HttpStatus.NOT_FOUND);
    }
}
