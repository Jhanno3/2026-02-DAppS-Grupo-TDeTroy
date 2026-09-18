package com.tdetroy.valuacion.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.assertj.MockMvcTester.create;

import com.tdetroy.valuacion.config.JwtService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Test de integración de {@link AuthController} (tasks.md T1.5) contra el contexto Spring completo
 * (Postgres real, la cadena de seguridad real de {@code SecurityConfig}, {@code
 * GlobalExceptionHandler}) — es el primer Controller del proyecto, así que es la primera vez que se
 * prueba de punta a punta que {@code UsuarioService} + BCrypt + {@code AuthenticationManager} +
 * {@code JwtService} quedan bien enganchados entre sí, algo que los tests unitarios de T1.3/T1.4 no
 * pueden ver por separado.
 *
 * <p>{@code @Transactional} hace rollback de cada test (misma convención que cualquier test que
 * persiste contra la base de desarrollo real, evita ensuciarla entre corridas); las llamadas vía
 * {@code MockMvc} corren en el mismo hilo/transacción que el test porque no hay una conexión de red
 * real de por medio.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvcTester mvc;

    @Autowired
    void inicializarMvcTester(MockMvc mockMvc) {
        this.mvc = create(mockMvc);
    }

    private static String emailUnico() {
        return "test-" + UUID.randomUUID() + "@example.com";
    }

    private static String requestJson(String email, String password) {
        return "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
    }

    // ---- POST /auth/registro (UC-01) ----

    @Test
    void registro_conDatosValidos_creaLaCuentaConRolUserYSaldoCero() {
        String email = emailUnico();

        mvc.post()
                .uri("/api/v1/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(email, "contrasenia-segura"))
                .assertThat()
                .hasStatus(201)
                .bodyText()
                .contains("\"email\":\"" + email + "\"")
                .contains("\"rol\":\"USER\"")
                .contains("\"saldoVirtual\":0")
                .doesNotContain("password");
    }

    @Test
    void registro_conEmailYaRegistrado_responde409SinCrearOtraCuenta() {
        String email = emailUnico();
        String body = requestJson(email, "contrasenia-segura");
        registrar(email, "contrasenia-segura");

        mvc.post()
                .uri("/api/v1/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .assertThat()
                .hasStatus(409)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyText()
                .contains(email);
    }

    @Test
    void registro_conEmailInvalido_responde400() {
        mvc.post()
                .uri("/api/v1/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson("no-es-un-email", "contrasenia-segura"))
                .assertThat()
                .hasStatus(400);
    }

    @Test
    void registro_conPasswordBlanco_responde400() {
        mvc.post()
                .uri("/api/v1/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(emailUnico(), " "))
                .assertThat()
                .hasStatus(400);
    }

    // ---- POST /auth/login (UC-02) ----

    @Test
    void login_conCredencialesCorrectas_devuelveUnJwtValidoConLosDatosDeLaCuenta() {
        String email = emailUnico();
        registrar(email, "contrasenia-correcta");

        MvcTestResult resultado = login(email, "contrasenia-correcta");
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);

        String token = extraerToken(resultado);
        var claims = jwtService.validarYExtraerClaims(token);
        assertThat(claims).isPresent();
        assertThat(claims.get().email()).isEqualTo(email);
    }

    @Test
    void login_conPasswordIncorrecto_responde401ConMensajeGenerico() {
        String email = emailUnico();
        registrar(email, "contrasenia-correcta");

        mvc.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(email, "contrasenia-incorrecta"))
                .assertThat()
                .hasStatus(401)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyText()
                .contains("Email o contraseña inválidos")
                .doesNotContain(email);
    }

    @Test
    void login_conEmailInexistente_responde401ConElMismoMensajeGenerico() {
        mvc.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(emailUnico(), "lo-que-sea"))
                .assertThat()
                .hasStatus(401)
                .bodyText()
                .contains("Email o contraseña inválidos")
                .doesNotContain("No existe una cuenta");
    }

    private void registrar(String email, String password) {
        mvc.post()
                .uri("/api/v1/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(email, password))
                .assertThat()
                .hasStatus(201);
    }

    private MvcTestResult login(String email, String password) {
        return mvc.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(email, password))
                .exchange();
    }

    private String extraerToken(MvcTestResult resultado) {
        try {
            var nodo = objectMapper.readTree(resultado.getResponse().getContentAsString());
            return nodo.get("token").asString();
        } catch (Exception ex) {
            throw new AssertionError("No se pudo parsear la respuesta de login", ex);
        }
    }
}
