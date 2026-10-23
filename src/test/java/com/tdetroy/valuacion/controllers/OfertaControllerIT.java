package com.tdetroy.valuacion.controllers;

import static org.springframework.test.web.servlet.assertj.MockMvcTester.create;

import com.tdetroy.valuacion.config.JwtService;
import com.tdetroy.valuacion.model.Jugador;
import com.tdetroy.valuacion.model.Usuario;
import com.tdetroy.valuacion.services.JugadorService;
import com.tdetroy.valuacion.services.UsuarioService;
import java.math.BigDecimal;
import java.time.LocalDate;
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
 * Test de integración de {@link OfertaController} (tasks.md T6.3/T6.4/T6.5) contra el contexto
 * Spring completo — mismo criterio que {@code TokenControllerIT}: usuarios y jugador armados vía
 * {@code UsuarioService}/{@code JugadorService} autowireados, tenencia del vendedor armada
 * comprando contra el sistema (vía {@code TokenController} real, {@code MockMvc}), para probar de
 * punta a punta que {@code SecurityConfig} (GET público, POST/DELETE autenticados), {@code
 * OfertaService} y {@code GlobalExceptionHandler} quedan bien enganchados en el flujo P2P completo
 * (UC-17).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OfertaControllerIT {

    @Autowired private JwtService jwtService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private JugadorService jugadorService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvcTester mvc;

    @Autowired
    void inicializarMvcTester(MockMvc mockMvc) {
        this.mvc = create(mockMvc);
    }

    private Usuario crearUsuarioConSaldo(BigDecimal saldo) {
        Usuario usuario =
                usuarioService.registrar(
                        "test-" + UUID.randomUUID() + "@example.com", "hash-bcrypt");
        if (saldo.signum() > 0) {
            usuarioService.recargarSaldo(usuario.getId(), saldo, UUID.randomUUID());
        }
        return usuario;
    }

    private Jugador crearJugador() {
        return jugadorService.darAlta(
                "Jugador " + UUID.randomUUID(),
                "Club",
                "Delantero",
                LocalDate.of(2000, 1, 1),
                "Argentina",
                "Liga de prueba",
                UUID.randomUUID());
    }

    private String tokenDe(Usuario usuario) {
        return jwtService.generarToken(usuario.getId(), usuario.getEmail(), usuario.getRol());
    }

    /** Compra {@code cantidad} tokens al sistema vía {@code TokenController} real (MockMvc). */
    private void comprarAlSistema(Usuario usuario, Jugador jugador, int cantidad) {
        mvc.post()
                .uri("/api/v1/jugadores/{id}/compras", jugador.getId())
                .header("Authorization", "Bearer " + tokenDe(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":%d}".formatted(cantidad))
                .assertThat()
                .hasStatus(201);
    }

    private String publicarOfertaBody(UUID jugadorId, int cantidad) {
        return "{\"jugadorId\":\"%s\",\"cantidad\":%d,\"precioUnitario\":20.00,\"horasLimite\":24}"
                .formatted(jugadorId, cantidad);
    }

    private String publicarYObtenerId(Usuario vendedor, UUID jugadorId, int cantidad) {
        MvcTestResult resultado =
                mvc.post()
                        .uri("/api/v1/ofertas")
                        .header("Authorization", "Bearer " + tokenDe(vendedor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publicarOfertaBody(jugadorId, cantidad))
                        .exchange();
        try {
            var nodo = objectMapper.readTree(resultado.getResponse().getContentAsString());
            return nodo.get("id").asString();
        } catch (Exception ex) {
            throw new AssertionError("No se pudo parsear la respuesta de publicar", ex);
        }
    }

    // ---- POST /ofertas (UC-17, T6.2/T6.3) ----

    @Test
    void publicar_sinToken_responde401() {
        Jugador jugador = crearJugador();

        mvc.post()
                .uri("/api/v1/ofertas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(publicarOfertaBody(jugador.getId(), 1))
                .assertThat()
                .hasStatus(401);
    }

    @Test
    void publicar_conTenenciaSuficiente_reservaYResponde201() {
        Usuario vendedor = crearUsuarioConSaldo(new BigDecimal("1000.00"));
        Jugador jugador = crearJugador();
        comprarAlSistema(vendedor, jugador, 10);

        mvc.post()
                .uri("/api/v1/ofertas")
                .header("Authorization", "Bearer " + tokenDe(vendedor))
                .contentType(MediaType.APPLICATION_JSON)
                .content(publicarOfertaBody(jugador.getId(), 6))
                .assertThat()
                .hasStatus(201)
                .bodyText()
                .contains("\"cantidadPublicada\":6")
                .contains("\"cantidadDisponible\":6")
                .contains("\"estado\":\"ABIERTA\"");
    }

    @Test
    void publicar_sinTenencia_responde409() {
        Usuario vendedor = crearUsuarioConSaldo(BigDecimal.ZERO);
        Jugador jugador = crearJugador();

        mvc.post()
                .uri("/api/v1/ofertas")
                .header("Authorization", "Bearer " + tokenDe(vendedor))
                .contentType(MediaType.APPLICATION_JSON)
                .content(publicarOfertaBody(jugador.getId(), 1))
                .assertThat()
                .hasStatus(409)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void publicar_conJugadorInexistente_responde404() {
        Usuario vendedor = crearUsuarioConSaldo(BigDecimal.ZERO);

        mvc.post()
                .uri("/api/v1/ofertas")
                .header("Authorization", "Bearer " + tokenDe(vendedor))
                .contentType(MediaType.APPLICATION_JSON)
                .content(publicarOfertaBody(UUID.randomUUID(), 1))
                .assertThat()
                .hasStatus(404)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    // ---- GET /ofertas?jugadorId={id} (UC-17, T6.3, público) ----

    @Test
    void listar_sinAutenticacion_devuelveLasOfertasAbiertasDelJugador() {
        Usuario vendedor = crearUsuarioConSaldo(new BigDecimal("1000.00"));
        Jugador jugador = crearJugador();
        comprarAlSistema(vendedor, jugador, 10);
        String ofertaId = publicarYObtenerId(vendedor, jugador.getId(), 5);

        mvc.get()
                .uri("/api/v1/ofertas?jugadorId={id}", jugador.getId())
                .assertThat()
                .hasStatus(200)
                .bodyText()
                .contains("\"id\":\"" + ofertaId + "\"")
                .contains("\"estado\":\"ABIERTA\"");
    }

    @Test
    void listar_sinOfertas_devuelveListaVacia() {
        Jugador jugador = crearJugador();

        mvc.get()
                .uri("/api/v1/ofertas?jugadorId={id}", jugador.getId())
                .assertThat()
                .hasStatus(200)
                .bodyText()
                .isEqualTo("[]");
    }

    // ---- POST /ofertas/{id}/compras (UC-17, T6.4) ----

    @Test
    void comprar_sinToken_responde401() {
        Usuario vendedor = crearUsuarioConSaldo(new BigDecimal("1000.00"));
        Jugador jugador = crearJugador();
        comprarAlSistema(vendedor, jugador, 10);
        String ofertaId = publicarYObtenerId(vendedor, jugador.getId(), 5);

        mvc.post()
                .uri("/api/v1/ofertas/{id}/compras", ofertaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":1}")
                .assertThat()
                .hasStatus(401);
    }

    @Test
    void
            comprar_conCantidadYSaldoSuficientes_ejecutaLaOperacionYResponde201ConElMovimientoDelComprador() {
        Usuario vendedor = crearUsuarioConSaldo(new BigDecimal("1000.00"));
        Usuario comprador = crearUsuarioConSaldo(new BigDecimal("1000.00"));
        Jugador jugador = crearJugador();
        comprarAlSistema(vendedor, jugador, 10);
        String ofertaId = publicarYObtenerId(vendedor, jugador.getId(), 6);

        mvc.post()
                .uri("/api/v1/ofertas/{id}/compras", ofertaId)
                .header("Authorization", "Bearer " + tokenDe(comprador))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":4}")
                .assertThat()
                .hasStatus(201)
                .bodyText()
                .contains("\"tipo\":\"COMPRA_P2P\"")
                .contains("\"cantidad\":4")
                .contains("\"contraparteUsuarioId\":\"" + vendedor.getId() + "\"");

        mvc.get()
                .uri("/api/v1/ofertas?jugadorId={id}", jugador.getId())
                .assertThat()
                .bodyText()
                .contains("\"cantidadDisponible\":2");
    }

    @Test
    void comprar_conCantidadQueSuperaLaDisponible_responde409() {
        Usuario vendedor = crearUsuarioConSaldo(new BigDecimal("1000.00"));
        Usuario comprador = crearUsuarioConSaldo(new BigDecimal("1000.00"));
        Jugador jugador = crearJugador();
        comprarAlSistema(vendedor, jugador, 10);
        String ofertaId = publicarYObtenerId(vendedor, jugador.getId(), 3);

        mvc.post()
                .uri("/api/v1/ofertas/{id}/compras", ofertaId)
                .header("Authorization", "Bearer " + tokenDe(comprador))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":4}")
                .assertThat()
                .hasStatus(409)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void comprar_conSaldoInsuficienteDelComprador_responde409() {
        Usuario vendedor = crearUsuarioConSaldo(new BigDecimal("1000.00"));
        Usuario comprador = crearUsuarioConSaldo(new BigDecimal("1.00"));
        Jugador jugador = crearJugador();
        comprarAlSistema(vendedor, jugador, 10);
        String ofertaId = publicarYObtenerId(vendedor, jugador.getId(), 5);

        mvc.post()
                .uri("/api/v1/ofertas/{id}/compras", ofertaId)
                .header("Authorization", "Bearer " + tokenDe(comprador))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":5}")
                .assertThat()
                .hasStatus(409)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void comprar_conOfertaInexistente_responde404() {
        Usuario comprador = crearUsuarioConSaldo(new BigDecimal("1000.00"));

        mvc.post()
                .uri("/api/v1/ofertas/{id}/compras", UUID.randomUUID())
                .header("Authorization", "Bearer " + tokenDe(comprador))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":1}")
                .assertThat()
                .hasStatus(404)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    // ---- DELETE /ofertas/{id} (UC-17, T6.5) ----

    @Test
    void cancelar_sinToken_responde401() {
        mvc.delete().uri("/api/v1/ofertas/{id}", UUID.randomUUID()).assertThat().hasStatus(401);
    }

    @Test
    void cancelar_conDuenioYOfertaAbierta_liberaLaReservaYResponde204() {
        Usuario vendedor = crearUsuarioConSaldo(new BigDecimal("1000.00"));
        Jugador jugador = crearJugador();
        comprarAlSistema(vendedor, jugador, 10);
        String ofertaId = publicarYObtenerId(vendedor, jugador.getId(), 6);

        mvc.delete()
                .uri("/api/v1/ofertas/{id}", ofertaId)
                .header("Authorization", "Bearer " + tokenDe(vendedor))
                .assertThat()
                .hasStatus(204);

        mvc.get()
                .uri("/api/v1/ofertas?jugadorId={id}", jugador.getId())
                .assertThat()
                .bodyText()
                .isEqualTo("[]");

        // La cantidad vuelve a estar disponible como tenencia ordinaria: se puede vender al
        // sistema sin que la reserva previa lo bloquee (spec.md UC-17).
        mvc.post()
                .uri("/api/v1/jugadores/{id}/ventas", jugador.getId())
                .header("Authorization", "Bearer " + tokenDe(vendedor))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":10}")
                .assertThat()
                .hasStatus(201);
    }

    @Test
    void cancelar_conOfertaDeOtroVendedor_responde409() {
        Usuario vendedor = crearUsuarioConSaldo(new BigDecimal("1000.00"));
        Usuario otroUsuario = crearUsuarioConSaldo(BigDecimal.ZERO);
        Jugador jugador = crearJugador();
        comprarAlSistema(vendedor, jugador, 10);
        String ofertaId = publicarYObtenerId(vendedor, jugador.getId(), 5);

        mvc.delete()
                .uri("/api/v1/ofertas/{id}", ofertaId)
                .header("Authorization", "Bearer " + tokenDe(otroUsuario))
                .assertThat()
                .hasStatus(409)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void cancelar_conOfertaYaCancelada_responde409() {
        Usuario vendedor = crearUsuarioConSaldo(new BigDecimal("1000.00"));
        Jugador jugador = crearJugador();
        comprarAlSistema(vendedor, jugador, 10);
        String ofertaId = publicarYObtenerId(vendedor, jugador.getId(), 5);
        mvc.delete()
                .uri("/api/v1/ofertas/{id}", ofertaId)
                .header("Authorization", "Bearer " + tokenDe(vendedor))
                .assertThat()
                .hasStatus(204);

        mvc.delete()
                .uri("/api/v1/ofertas/{id}", ofertaId)
                .header("Authorization", "Bearer " + tokenDe(vendedor))
                .assertThat()
                .hasStatus(409)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void cancelar_conOfertaInexistente_responde404() {
        Usuario vendedor = crearUsuarioConSaldo(BigDecimal.ZERO);

        mvc.delete()
                .uri("/api/v1/ofertas/{id}", UUID.randomUUID())
                .header("Authorization", "Bearer " + tokenDe(vendedor))
                .assertThat()
                .hasStatus(404)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }
}
