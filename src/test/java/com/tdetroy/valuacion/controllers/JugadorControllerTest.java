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
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Test de integración de {@link JugadorController} (tasks.md T2.5) contra el contexto Spring
 * completo — igual criterio que {@code AuthControllerTest} (T1.5): prueba de punta a punta que
 * {@code SecurityConfig} (whitelist de {@code GET}, {@code @PreAuthorize("hasRole('ADMIN')")} en
 * {@code POST}/{@code PUT}), {@code JugadorService} y {@code GlobalExceptionHandler} quedan bien
 * enganchados entre sí.
 *
 * <p>Los tokens ADMIN/USER se generan directamente vía {@link JwtService#generarToken(UUID, String,
 * RolUsuario)} sin persistir un {@code Usuario} — {@link
 * com.tdetroy.valuacion.config.JwtAuthenticationFilter} resuelve la autenticación sólo desde los
 * claims del token, nunca contra la base (ver su javadoc), así que alcanza con un JWT válido con el
 * rol que cada test necesita.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JugadorControllerTest {

    @Autowired private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();
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

    private static String requestJson(String nombre) {
        return """
                {"nombre":"%s","club":"Inter Miami CF","posicion":"Delantero",\
                "fechaNacimiento":"1987-06-24","nacionalidad":"Argentina"}"""
                .formatted(nombre);
    }

    private MvcTestResult crear(String token, String body) {
        return mvc.post()
                .uri("/api/v1/jugadores")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    private MvcTestResult crearSinToken(String body) {
        return mvc.post()
                .uri("/api/v1/jugadores")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    private String crearYObtenerId(String nombre) {
        MvcTestResult resultado = crear(tokenAdmin(), requestJson(nombre));
        try {
            var nodo = objectMapper.readTree(resultado.getResponse().getContentAsString());
            return nodo.get("id").asString();
        } catch (Exception ex) {
            throw new AssertionError("No se pudo parsear la respuesta de alta", ex);
        }
    }

    // ---- GET /jugadores, GET /jugadores/{id} (UC-07, público) ----

    @Test
    void listar_sinAutenticacion_devuelveElCatalogoIncluyendoElJugadorRecienCreado() {
        String nombre = "Jugador Catalogo " + UUID.randomUUID();
        crearYObtenerId(nombre);

        mvc.get().uri("/api/v1/jugadores").assertThat().hasStatus(200).bodyText().contains(nombre);
    }

    @Test
    void obtenerPorId_sinAutenticacion_devuelveDetalleConTokensDisponiblesEnCien() {
        String id = crearYObtenerId("Jugador Detalle " + UUID.randomUUID());

        mvc.get()
                .uri("/api/v1/jugadores/{id}", id)
                .assertThat()
                .hasStatus(200)
                .bodyText()
                .contains("\"tokensDisponibles\":100")
                .contains("\"estado\":\"ACTIVO\"");
    }

    @Test
    void obtenerPorId_conJugadorInexistente_responde404() {
        mvc.get()
                .uri("/api/v1/jugadores/{id}", UUID.randomUUID())
                .assertThat()
                .hasStatus(404)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    // ---- POST /jugadores (UC-03, ADMIN) ----

    @Test
    void darAlta_sinToken_responde401() {
        crearSinToken(requestJson("Sin Token")).assertThat().hasStatus(401);
    }

    @Test
    void darAlta_conTokenUser_responde403() {
        mvc.post()
                .uri("/api/v1/jugadores")
                .header("Authorization", "Bearer " + tokenUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson("Con Rol User"))
                .assertThat()
                .hasStatus(403);
    }

    @Test
    void darAlta_conTokenAdmin_creaYResponde201ConTokensDisponiblesEnCien() {
        crear(tokenAdmin(), requestJson("Con Rol Admin"))
                .assertThat()
                .hasStatus(201)
                .bodyText()
                .contains("\"estado\":\"ACTIVO\"")
                .contains("\"tokensDisponibles\":100");
    }

    @Test
    void darAlta_conNombreBlanco_responde400() {
        String body =
                """
                {"nombre":"   ","club":"Club","posicion":"Delantero",\
                "fechaNacimiento":"1987-06-24","nacionalidad":"Argentina"}""";

        crear(tokenAdmin(), body).assertThat().hasStatus(400);
    }

    // ---- PUT /jugadores/{id} (UC-04, ADMIN) ----

    @Test
    void editar_conTokenAdmin_actualizaDatosSinTocarEstadoNiTokensDisponibles() {
        String id = crearYObtenerId("Nombre Original " + UUID.randomUUID());
        String bodyEdicion =
                """
                {"nombre":"Nombre Editado","club":"Club Editado","posicion":"Mediocampista",\
                "fechaNacimiento":"1990-05-10","nacionalidad":"Brasil"}""";

        mvc.put()
                .uri("/api/v1/jugadores/{id}", id)
                .header("Authorization", "Bearer " + tokenAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyEdicion)
                .assertThat()
                .hasStatus(200)
                .bodyText()
                .contains("\"nombre\":\"Nombre Editado\"")
                .contains("\"club\":\"Club Editado\"")
                .contains("\"estado\":\"ACTIVO\"")
                .contains("\"tokensDisponibles\":100");
    }

    @Test
    void editar_conTokenUser_responde403() {
        String id = crearYObtenerId("Jugador Para Editar " + UUID.randomUUID());

        mvc.put()
                .uri("/api/v1/jugadores/{id}", id)
                .header("Authorization", "Bearer " + tokenUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson("Intento De Edicion"))
                .assertThat()
                .hasStatus(403);
    }

    @Test
    void editar_conJugadorInexistente_responde404() {
        mvc.put()
                .uri("/api/v1/jugadores/{id}", UUID.randomUUID())
                .header("Authorization", "Bearer " + tokenAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson("No Existe"))
                .assertThat()
                .hasStatus(404)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }
}
