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
 * Test de integración de {@link PortfolioController} (tasks.md T5.4) contra el contexto Spring
 * completo — mismo criterio que {@code TokenControllerIT}: usuario/jugador armados vía {@code
 * UsuarioService}/{@code JugadorService} autowireados, tenencia armada comprando contra {@code
 * TokenController} real (vía {@code MockMvc}), para probar de punta a punta que {@code
 * SecurityConfig} y {@code PortfolioService} quedan bien enganchados.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PortfolioControllerIT {

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
    void obtenerMiPortfolio_sinToken_responde401() {
        mvc.get().uri("/api/v1/portfolios/me").assertThat().hasStatus(401);
    }

    @Test
    void obtenerMiPortfolio_sinTenencias_devuelveListaVaciaConElSaldo() {
        Usuario usuario = crearUsuarioConSaldo(new BigDecimal("50.00"));

        mvc.get()
                .uri("/api/v1/portfolios/me")
                .header("Authorization", "Bearer " + tokenDe(usuario))
                .assertThat()
                .hasStatus(200)
                .bodyText()
                .contains("\"saldoVirtual\":50.00")
                .contains("\"tenencias\":[]")
                .contains("\"valorTotal\":50.00");
    }

    @Test
    void obtenerMiPortfolio_conUnaCompraPrevia_reflejaLaTenenciaYElValorTotal() {
        Usuario usuario = crearUsuarioConSaldo(new BigDecimal("1000.00"));
        Jugador jugador = crearJugador();
        String token = tokenDe(usuario);
        mvc.post()
                .uri("/api/v1/jugadores/{id}/compras", jugador.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":4}")
                .assertThat()
                .hasStatus(201);

        // jugador recién creado, sin cotización calculada -> cae al valor inicial (100.00):
        // saldo restante 600.00 + tenencia 4*100.00=400.00 = 1000.00
        mvc.get()
                .uri("/api/v1/portfolios/me")
                .header("Authorization", "Bearer " + token)
                .assertThat()
                .hasStatus(200)
                .bodyText()
                .contains("\"jugadorId\":\"" + jugador.getId() + "\"")
                .contains("\"cantidad\":4")
                .contains("\"saldoVirtual\":600.00")
                .contains("\"valorTotal\":1000.00");
    }
}
