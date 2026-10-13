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
 * Test de integración de {@link MovimientoController} (tasks.md T5.6) contra el contexto Spring
 * completo — mismo criterio que {@code PortfolioControllerIT}: usuario armado vía {@code
 * UsuarioService} autowireado, movimientos armados comprando contra {@code TokenController} real
 * (vía {@code MockMvc}), para probar de punta a punta que {@code SecurityConfig} y {@code
 * MovimientoService} quedan bien enganchados — en particular, que cada usuario sólo ve su propio
 * historial (spec.md UC-12).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MovimientoControllerIT {

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
                UUID.randomUUID());
    }

    private String tokenDe(Usuario usuario) {
        return jwtService.generarToken(usuario.getId(), usuario.getEmail(), usuario.getRol());
    }

    @Test
    void obtenerMiHistorial_sinToken_responde401() {
        mvc.get().uri("/api/v1/movimientos/me").assertThat().hasStatus(401);
    }

    @Test
    void obtenerMiHistorial_sinMovimientos_devuelveListaVacia() {
        Usuario usuario = crearUsuarioConSaldo(BigDecimal.ZERO);

        mvc.get()
                .uri("/api/v1/movimientos/me")
                .header("Authorization", "Bearer " + tokenDe(usuario))
                .assertThat()
                .hasStatus(200)
                .bodyText()
                .isEqualTo("[]");
    }

    @Test
    void obtenerMiHistorial_conUnaCompraPrevia_muestraEseMovimiento() {
        Usuario usuario = crearUsuarioConSaldo(new BigDecimal("1000.00"));
        Jugador jugador = crearJugador();
        String token = tokenDe(usuario);
        mvc.post()
                .uri("/api/v1/jugadores/{id}/compras", jugador.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":2}")
                .assertThat()
                .hasStatus(201);

        mvc.get()
                .uri("/api/v1/movimientos/me")
                .header("Authorization", "Bearer " + token)
                .assertThat()
                .hasStatus(200)
                .bodyText()
                .contains("\"tipo\":\"COMPRA_SISTEMA\"")
                .contains("\"jugadorId\":\"" + jugador.getId() + "\"")
                .contains("\"cantidad\":2");
    }

    @Test
    void obtenerMiHistorial_deUnUsuario_nuncaIncluyeMovimientosDeOtroUsuario() {
        Usuario usuarioUno = crearUsuarioConSaldo(new BigDecimal("1000.00"));
        Usuario usuarioDos = crearUsuarioConSaldo(BigDecimal.ZERO);
        Jugador jugador = crearJugador();
        mvc.post()
                .uri("/api/v1/jugadores/{id}/compras", jugador.getId())
                .header("Authorization", "Bearer " + tokenDe(usuarioUno))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":1}")
                .assertThat()
                .hasStatus(201);

        mvc.get()
                .uri("/api/v1/movimientos/me")
                .header("Authorization", "Bearer " + tokenDe(usuarioDos))
                .assertThat()
                .hasStatus(200)
                .bodyText()
                .isEqualTo("[]");
    }
}
