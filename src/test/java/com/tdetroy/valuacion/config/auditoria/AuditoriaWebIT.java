package com.tdetroy.valuacion.config.auditoria;

import static org.assertj.core.api.Assertions.assertThat;

import com.tdetroy.valuacion.common.CorrelationId;
import com.tdetroy.valuacion.config.seguridad.JwtService;
import com.tdetroy.valuacion.model.usuario.RolUsuario;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** El archivo de auditoría que escribe la aplicación completa. */
@SpringBootTest
@AutoConfigureMockMvc
class AuditoriaWebIT {

    @Value("${app.auditoria.archivo}")
    private Path archivo;

    @Autowired private JwtService jwtService;
    @Autowired private JsonMapper jsonMapper;
    private final String correlationId = UUID.randomUUID().toString();
    private MockMvcTester mvc;

    @Autowired
    void inicializarMvcTester(MockMvc mockMvc) {
        this.mvc = MockMvcTester.create(mockMvc);
    }

    private String lineaAuditada() throws IOException {
        List<String> lineas =
                Files.readAllLines(archivo).stream()
                        .filter(linea -> linea.contains(correlationId))
                        .toList();
        assertThat(lineas).hasSize(1);
        return lineas.getFirst();
    }

    @Test
    void login_noGuardaLasCredenciales() throws IOException {
        assertThat(
                        mvc.post()
                                .uri("/api/v1/auth/login")
                                .header(CorrelationId.HEADER, correlationId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"nadie@example.com\",\"password\":\"MuySecreta1!\"}")
                                .exchange())
                .hasStatus(HttpStatus.UNAUTHORIZED);

        String linea = lineaAuditada();
        assertThat(linea).doesNotContain("MuySecreta1!", "nadie@example.com");
        JsonNode json = jsonMapper.readTree(linea);
        assertThat(json.get("@timestamp").asString()).isNotBlank();
        assertThat(json.get("usuario").asString()).isEqualTo("anonimo");
        assertThat(json.get("metodo").asString()).isEqualTo("AuthController.login");
        assertThat(json.at("/parametros/request/password").asString()).isEqualTo("***");
        assertThat(json.get("resultado").asString()).isEqualTo("BadCredentialsException");
        assertThat(json.get("duracionMs").isNumber()).isTrue();
    }

    @Test
    void unRechazoPorRol_seAuditaConElUsuario() throws IOException {
        UUID usuarioId = UUID.randomUUID();
        String token = jwtService.generarToken(usuarioId, "user@example.com", RolUsuario.USER);

        assertThat(
                        mvc.post()
                                .uri("/api/v1/jugadores/{id}/baja", UUID.randomUUID())
                                .header("Authorization", "Bearer " + token)
                                .header(CorrelationId.HEADER, correlationId)
                                .exchange())
                .hasStatus(HttpStatus.FORBIDDEN);

        JsonNode json = jsonMapper.readTree(lineaAuditada());
        assertThat(json.get("usuario").asString()).isEqualTo(usuarioId.toString());
        assertThat(json.get("metodo").asString()).isEqualTo("JugadorController.darDeBaja");
        assertThat(json.get("resultado").asString()).isEqualTo("AuthorizationDeniedException");
    }

    @Test
    void unaConsultaExitosa_seAudita() throws IOException {
        assertThat(
                        mvc.get()
                                .uri("/api/v1/jugadores/ranking?estrategia=cotizacion")
                                .header(CorrelationId.HEADER, correlationId)
                                .exchange())
                .hasStatusOk();

        JsonNode json = jsonMapper.readTree(lineaAuditada());
        assertThat(json.get("resultado").asString()).isEqualTo("OK");
    }
}
