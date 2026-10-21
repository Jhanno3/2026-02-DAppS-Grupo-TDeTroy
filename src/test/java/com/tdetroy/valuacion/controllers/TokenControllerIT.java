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
import org.springframework.transaction.annotation.Transactional;

/**
 * Test de integración de {@link TokenController} (tasks.md T5.2) contra el contexto Spring completo
 * — mismo criterio que {@code JugadorControllerIT}: prueba de punta a punta que {@code
 * SecurityConfig} (requiere autenticación, cualquier rol, sin {@code @PreAuthorize}), {@code
 * TokenService} y {@code GlobalExceptionHandler} quedan bien enganchados.
 *
 * <p>El usuario y el jugador se arman vía {@code UsuarioService}/{@code JugadorService}
 * autowireados directamente (no vía HTTP): no existe todavía un endpoint de recarga de saldo
 * (tasks.md T1.6, {@code UsuarioController}) para llegar a ese estado sólo con {@code MockMvc}. El
 * JWT se emite con el {@code id} real del usuario persistido, igual que lo haría {@code
 * AuthController} en un login real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TokenControllerIT {

    @Autowired private JwtService jwtService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private JugadorService jugadorService;

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

    // ---- POST /jugadores/{id}/compras (UC-09) ----

    @Test
    void comprar_sinToken_responde401() {
        Jugador jugador = crearJugador();

        mvc.post()
                .uri("/api/v1/jugadores/{id}/compras", jugador.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":1}")
                .assertThat()
                .hasStatus(401);
    }

    @Test
    void comprar_conSaldoSuficiente_debitaSaldoYResponde201ConElMovimiento() {
        Usuario usuario = crearUsuarioConSaldo(new BigDecimal("1000.00"));
        Jugador jugador = crearJugador();

        mvc.post()
                .uri("/api/v1/jugadores/{id}/compras", jugador.getId())
                .header("Authorization", "Bearer " + tokenDe(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":3}")
                .assertThat()
                .hasStatus(201)
                .bodyText()
                .contains("\"tipo\":\"COMPRA_SISTEMA\"")
                .contains("\"cantidad\":3")
                .contains("\"jugadorId\":\"" + jugador.getId() + "\"");
    }

    @Test
    void comprar_conSaldoInsuficiente_responde409SinDebitarNada() {
        Usuario usuario = crearUsuarioConSaldo(new BigDecimal("1.00"));
        Jugador jugador = crearJugador();

        mvc.post()
                .uri("/api/v1/jugadores/{id}/compras", jugador.getId())
                .header("Authorization", "Bearer " + tokenDe(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":5}")
                .assertThat()
                .hasStatus(409)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void comprar_conJugadorInexistente_responde404() {
        Usuario usuario = crearUsuarioConSaldo(new BigDecimal("1000.00"));

        mvc.post()
                .uri("/api/v1/jugadores/{id}/compras", UUID.randomUUID())
                .header("Authorization", "Bearer " + tokenDe(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":1}")
                .assertThat()
                .hasStatus(404)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void comprar_conCantidadCero_responde400() {
        Usuario usuario = crearUsuarioConSaldo(new BigDecimal("1000.00"));
        Jugador jugador = crearJugador();

        mvc.post()
                .uri("/api/v1/jugadores/{id}/compras", jugador.getId())
                .header("Authorization", "Bearer " + tokenDe(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":0}")
                .assertThat()
                .hasStatus(400);
    }

    // ---- POST /jugadores/{id}/ventas (UC-10) ----

    @Test
    void vender_sinToken_responde401() {
        Jugador jugador = crearJugador();

        mvc.post()
                .uri("/api/v1/jugadores/{id}/ventas", jugador.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":1}")
                .assertThat()
                .hasStatus(401);
    }

    @Test
    void vender_conTenenciaPrevia_acreditaSaldoYResponde201ConElMovimiento() {
        Usuario usuario = crearUsuarioConSaldo(new BigDecimal("1000.00"));
        Jugador jugador = crearJugador();
        String token = tokenDe(usuario);
        mvc.post()
                .uri("/api/v1/jugadores/{id}/compras", jugador.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":5}")
                .assertThat()
                .hasStatus(201);

        mvc.post()
                .uri("/api/v1/jugadores/{id}/ventas", jugador.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":2}")
                .assertThat()
                .hasStatus(201)
                .bodyText()
                .contains("\"tipo\":\"VENTA_SISTEMA\"")
                .contains("\"cantidad\":2");
    }

    @Test
    void vender_sinTenencia_responde409() {
        Usuario usuario = crearUsuarioConSaldo(BigDecimal.ZERO);
        Jugador jugador = crearJugador();

        mvc.post()
                .uri("/api/v1/jugadores/{id}/ventas", jugador.getId())
                .header("Authorization", "Bearer " + tokenDe(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":1}")
                .assertThat()
                .hasStatus(409)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }
}
