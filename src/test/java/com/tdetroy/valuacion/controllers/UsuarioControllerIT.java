package com.tdetroy.valuacion.controllers;

import static org.springframework.test.web.servlet.assertj.MockMvcTester.create;

import com.tdetroy.valuacion.config.JwtService;
import com.tdetroy.valuacion.model.RolUsuario;
import com.tdetroy.valuacion.model.Usuario;
import com.tdetroy.valuacion.services.UsuarioService;
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
 * Test de integración de {@link UsuarioController} (tasks.md T1.6) contra el contexto Spring
 * completo — mismo criterio que {@code JugadorControllerIT}: prueba de punta a punta que {@code
 * SecurityConfig} ({@code @PreAuthorize("hasRole('ADMIN')")}), {@code UsuarioService} y {@code
 * GlobalExceptionHandler} quedan bien enganchados entre sí. El token ADMIN se genera directamente
 * vía {@link JwtService#generarToken(UUID, String, RolUsuario)} sin persistir ese {@code Usuario}
 * (mismo criterio que {@code JugadorControllerIT}: {@code JwtAuthenticationFilter} resuelve la
 * autenticación sólo desde los claims del token) — el usuario destino de la recarga sí se persiste,
 * porque {@code UsuarioService#recargarSaldo} necesita encontrarlo por id.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UsuarioControllerIT {

    @Autowired private JwtService jwtService;
    @Autowired private UsuarioService usuarioService;

    private MockMvcTester mvc;

    @Autowired
    void inicializarMvcTester(MockMvc mockMvc) {
        this.mvc = create(mockMvc);
    }

    private Usuario crearUsuario() {
        return usuarioService.registrar(
                "usuario-" + UUID.randomUUID() + "@example.com", "hash-bcrypt");
    }

    private String tokenAdmin() {
        return jwtService.generarToken(UUID.randomUUID(), "admin@example.com", RolUsuario.ADMIN);
    }

    private String tokenUser() {
        return jwtService.generarToken(UUID.randomUUID(), "user@example.com", RolUsuario.USER);
    }

    @Test
    void recargarSaldo_sinToken_responde401() {
        Usuario usuario = crearUsuario();

        mvc.post()
                .uri("/api/v1/usuarios/{id}/saldo", usuario.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"monto\":100.00}")
                .assertThat()
                .hasStatus(401);
    }

    @Test
    void recargarSaldo_conTokenUser_responde403() {
        Usuario usuario = crearUsuario();

        mvc.post()
                .uri("/api/v1/usuarios/{id}/saldo", usuario.getId())
                .header("Authorization", "Bearer " + tokenUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"monto\":100.00}")
                .assertThat()
                .hasStatus(403);
    }

    @Test
    void recargarSaldo_conTokenAdmin_acreditaSaldoYResponde200ConElSaldoActualizado() {
        Usuario usuario = crearUsuario();

        mvc.post()
                .uri("/api/v1/usuarios/{id}/saldo", usuario.getId())
                .header("Authorization", "Bearer " + tokenAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"monto\":150.50}")
                .assertThat()
                .hasStatus(200)
                .bodyText()
                .contains("\"saldoVirtual\":150.50")
                .contains("\"id\":\"" + usuario.getId() + "\"");
    }

    @Test
    void recargarSaldo_conUsuarioInexistente_responde404() {
        mvc.post()
                .uri("/api/v1/usuarios/{id}/saldo", UUID.randomUUID())
                .header("Authorization", "Bearer " + tokenAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"monto\":100.00}")
                .assertThat()
                .hasStatus(404)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void recargarSaldo_conMontoCero_responde400() {
        Usuario usuario = crearUsuario();

        mvc.post()
                .uri("/api/v1/usuarios/{id}/saldo", usuario.getId())
                .header("Authorization", "Bearer " + tokenAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"monto\":0}")
                .assertThat()
                .hasStatus(400);
    }

    @Test
    void recargarSaldo_conMontoNegativo_responde400() {
        Usuario usuario = crearUsuario();

        mvc.post()
                .uri("/api/v1/usuarios/{id}/saldo", usuario.getId())
                .header("Authorization", "Bearer " + tokenAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"monto\":-10.00}")
                .assertThat()
                .hasStatus(400);
    }

    @Test
    void recargarSaldo_conMontoAusente_responde400() {
        Usuario usuario = crearUsuario();

        mvc.post()
                .uri("/api/v1/usuarios/{id}/saldo", usuario.getId())
                .header("Authorization", "Bearer " + tokenAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .assertThat()
                .hasStatus(400);
    }
}
