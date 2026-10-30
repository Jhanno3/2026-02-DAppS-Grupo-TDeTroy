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
 * Test de integración de {@link SubastaController} (UC-18) contra el contexto Spring completo,
 * mismo criterio que {@code OfertaControllerIT}: prueba que {@code SecurityConfig} (GET público,
 * POST/DELETE autenticados), {@code SubastaService} y {@code GlobalExceptionHandler} quedan bien
 * enganchados — crear, pujar (con devolución al superado), reglas rechazadas con 409, y cancelar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SubastaControllerIT {

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

    private String bearer(Usuario usuario) {
        return "Bearer "
                + jwtService.generarToken(usuario.getId(), usuario.getEmail(), usuario.getRol());
    }

    private void comprarAlSistema(Usuario usuario, Jugador jugador, int cantidad) {
        mvc.post()
                .uri("/api/v1/jugadores/{id}/compras", jugador.getId())
                .header("Authorization", bearer(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":%d}".formatted(cantidad))
                .assertThat()
                .hasStatus(201);
    }

    private String crearSubastaBody(UUID jugadorId, int cantidad) {
        return "{\"jugadorId\":\"%s\",\"cantidad\":%d,\"precioInicial\":50.00,\"duracionMinutos\":30}"
                .formatted(jugadorId, cantidad);
    }

    /** Vendedor con 3 tokens comprados al sistema que subasta 2 de ellos a 50.00 c/u. */
    private String crearSubasta(Usuario vendedor, Jugador jugador) {
        comprarAlSistema(vendedor, jugador, 3);
        MvcTestResult resultado =
                mvc.post()
                        .uri("/api/v1/subastas")
                        .header("Authorization", bearer(vendedor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearSubastaBody(jugador.getId(), 2))
                        .exchange();
        try {
            return objectMapper
                    .readTree(resultado.getResponse().getContentAsString())
                    .get("id")
                    .asString();
        } catch (Exception ex) {
            throw new AssertionError("No se pudo parsear la respuesta de crear subasta", ex);
        }
    }

    private void pujar(Usuario postor, String subastaId, int porcentaje, int statusEsperado) {
        mvc.post()
                .uri("/api/v1/subastas/{id}/pujas", subastaId)
                .header("Authorization", bearer(postor))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"porcentaje\":%d}".formatted(porcentaje))
                .assertThat()
                .hasStatus(statusEsperado);
    }

    private void assertSaldo(Usuario usuario, String esperado) {
        mvc.get()
                .uri("/api/v1/portfolios/me")
                .header("Authorization", bearer(usuario))
                .assertThat()
                .hasStatus(200)
                .bodyText()
                .contains("\"saldoVirtual\":" + esperado);
    }

    @Test
    void crear_sinToken_responde401() {
        Jugador jugador = crearJugador();

        mvc.post()
                .uri("/api/v1/subastas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(crearSubastaBody(jugador.getId(), 1))
                .assertThat()
                .hasStatus(401);
    }

    @Test
    void crear_sinTenencia_responde409() {
        Usuario vendedor = crearUsuarioConSaldo(BigDecimal.ZERO);
        Jugador jugador = crearJugador();

        mvc.post()
                .uri("/api/v1/subastas")
                .header("Authorization", bearer(vendedor))
                .contentType(MediaType.APPLICATION_JSON)
                .content(crearSubastaBody(jugador.getId(), 1))
                .assertThat()
                .hasStatus(409);
    }

    @Test
    void crear_conDuracionFueraDeRango_responde400() {
        Usuario vendedor = crearUsuarioConSaldo(BigDecimal.ZERO);
        Jugador jugador = crearJugador();

        mvc.post()
                .uri("/api/v1/subastas")
                .header("Authorization", bearer(vendedor))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        "{\"jugadorId\":\"%s\",\"cantidad\":1,\"precioInicial\":50.00,\"duracionMinutos\":0}"
                                .formatted(jugador.getId()))
                .assertThat()
                .hasStatus(400);
    }

    @Test
    void listarYDetalle_sonPublicos_yMuestranLasOpcionesDePuja() {
        Usuario vendedor = crearUsuarioConSaldo(new BigDecimal("1000.00"));
        Jugador jugador = crearJugador();
        String subastaId = crearSubasta(vendedor, jugador);

        mvc.get()
                .uri("/api/v1/subastas?jugadorId={id}", jugador.getId())
                .assertThat()
                .hasStatus(200)
                .bodyText()
                .contains("\"id\":\"" + subastaId + "\"")
                .contains("\"estado\":\"ACTIVA\"");

        // Listado global, sin filtrar por jugador.
        mvc.get()
                .uri("/api/v1/subastas")
                .assertThat()
                .hasStatus(200)
                .bodyText()
                .contains("\"id\":\"" + subastaId + "\"");

        mvc.get()
                .uri("/api/v1/subastas/{id}", subastaId)
                .assertThat()
                .hasStatus(200)
                .bodyText()
                // 0% (igualar), 5%, 10% y 20% sobre 50.00, por 2 tokens
                .contains("{\"porcentaje\":0,\"precioUnitario\":50.00,\"montoTotal\":100.00}")
                .contains("{\"porcentaje\":20,\"precioUnitario\":60.00,\"montoTotal\":120.00}")
                .contains("\"pujas\":[]");
    }

    @Test
    void pujar_descuentaAlPostorYDevuelveAlSuperado() {
        Usuario vendedor = crearUsuarioConSaldo(new BigDecimal("1000.00"));
        Usuario postorA = crearUsuarioConSaldo(new BigDecimal("500.00"));
        Usuario postorB = crearUsuarioConSaldo(new BigDecimal("500.00"));
        Jugador jugador = crearJugador();
        String subastaId = crearSubasta(vendedor, jugador);

        pujar(postorA, subastaId, 0, 200);
        assertSaldo(postorA, "400.00");

        pujar(postorB, subastaId, 10, 200);
        // A recupera sus 100.00; B reserva 2 × 55.00
        assertSaldo(postorA, "500.00");
        assertSaldo(postorB, "390.00");

        mvc.get()
                .uri("/api/v1/portfolios/me")
                .header("Authorization", bearer(postorB))
                .assertThat()
                .bodyText()
                .contains("\"saldoEnPujas\":110.00")
                .contains("\"valorTotal\":500.00");
    }

    @Test
    void pujar_reglasInvalidas_responden409() {
        Usuario vendedor = crearUsuarioConSaldo(new BigDecimal("1000.00"));
        Usuario postor = crearUsuarioConSaldo(new BigDecimal("500.00"));
        Usuario sinSaldo = crearUsuarioConSaldo(new BigDecimal("10.00"));
        Jugador jugador = crearJugador();
        String subastaId = crearSubasta(vendedor, jugador);

        pujar(vendedor, subastaId, 5, 409);
        pujar(postor, subastaId, 15, 409);
        pujar(sinSaldo, subastaId, 0, 409);

        pujar(postor, subastaId, 5, 200);
        pujar(postor, subastaId, 5, 409);
        // Ya hubo pujas: igualar el precio inicial no vale más.
        pujar(sinSaldo, subastaId, 0, 409);
    }

    @Test
    void pujar_sinToken_responde401() {
        Usuario vendedor = crearUsuarioConSaldo(new BigDecimal("1000.00"));
        Jugador jugador = crearJugador();
        String subastaId = crearSubasta(vendedor, jugador);

        mvc.post()
                .uri("/api/v1/subastas/{id}/pujas", subastaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"porcentaje\":5}")
                .assertThat()
                .hasStatus(401);
    }

    @Test
    void cancelar_porElVendedor_devuelveSaldoAlMejorPostor_yPorOtroResponde409() {
        Usuario vendedor = crearUsuarioConSaldo(new BigDecimal("1000.00"));
        Usuario postor = crearUsuarioConSaldo(new BigDecimal("500.00"));
        Jugador jugador = crearJugador();
        String subastaId = crearSubasta(vendedor, jugador);
        pujar(postor, subastaId, 5, 200);

        mvc.delete()
                .uri("/api/v1/subastas/{id}", subastaId)
                .header("Authorization", bearer(postor))
                .assertThat()
                .hasStatus(409);

        mvc.delete()
                .uri("/api/v1/subastas/{id}", subastaId)
                .header("Authorization", bearer(vendedor))
                .assertThat()
                .hasStatus(204);

        assertSaldo(postor, "500.00");
        mvc.get()
                .uri("/api/v1/subastas/{id}", subastaId)
                .assertThat()
                .bodyText()
                .contains("\"estado\":\"CANCELADA\"")
                .contains("\"opcionesPuja\":[]");
    }

    @Test
    void obtener_inexistente_responde404() {
        mvc.get().uri("/api/v1/subastas/{id}", UUID.randomUUID()).assertThat().hasStatus(404);
    }
}
