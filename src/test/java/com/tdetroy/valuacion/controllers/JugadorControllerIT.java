package com.tdetroy.valuacion.controllers;

import static org.springframework.test.web.servlet.assertj.MockMvcTester.create;

import com.tdetroy.valuacion.config.JwtService;
import com.tdetroy.valuacion.entity.CotizacionHistoricaEntity;
import com.tdetroy.valuacion.model.CotizacionHistorica;
import com.tdetroy.valuacion.model.EstrategiaCotizacion;
import com.tdetroy.valuacion.model.OrigenCotizacion;
import com.tdetroy.valuacion.model.RolUsuario;
import com.tdetroy.valuacion.model.Usuario;
import com.tdetroy.valuacion.repositories.CotizacionHistoricaRepository;
import com.tdetroy.valuacion.services.UsuarioService;
import java.math.BigDecimal;
import java.time.Instant;
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
 * completo — igual criterio que {@code AuthControllerIT} (T1.5): prueba de punta a punta que {@code
 * SecurityConfig} (whitelist de {@code GET}, {@code @PreAuthorize("hasRole('ADMIN')")} en {@code
 * POST}/{@code PUT}), {@code JugadorService} y {@code GlobalExceptionHandler} quedan bien
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
class JugadorControllerIT {

    @Autowired private JwtService jwtService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private CotizacionHistoricaRepository cotizacionHistoricaRepository;

    @Autowired
    private RendimientoAcumuladoWhoScoredRepository rendimientoAcumuladoWhoScoredRepository;

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
                "fechaNacimiento":"1987-06-24","nacionalidad":"Argentina",\
                "liga":"Major League Soccer"}"""
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

    // ---- GET /jugadores/mercado (UC-07, público, plan.md §3bis) ----
    //
    // Este Controller corre contra la base de desarrollo real (no una vacía por test, ver clase),
    // que a esta altura del proyecto ya tiene un catálogo real de miles de jugadores cargado --
    // cada test de /mercado filtra con `busqueda` por un nombre único (UUID incluido) para que los
    // resultados queden acotados a los jugadores que el propio test creó, sin importar cuántos
    // otros haya en el catálogo real ni en qué página quedarían sin ese filtro.

    @Test
    void mercado_sinAutenticacion_devuelveElJugadorRecienCreadoConPrecioNull() {
        String nombre = "Jugador Mercado " + UUID.randomUUID();
        crearYObtenerId(nombre);

        mvc.get()
                .uri("/api/v1/jugadores/mercado?busqueda={busqueda}", nombre)
                .assertThat()
                .hasStatus(200)
                .bodyText()
                .contains(nombre)
                .contains("\"precio\":null")
                .contains("\"posicionesDisponibles\"");
    }

    @Test
    void mercado_conTamanioUno_pagina() {
        String prefijo = "Jugador Mercado Pagina " + UUID.randomUUID();
        crearYObtenerId(prefijo + " A");
        crearYObtenerId(prefijo + " B");

        mvc.get()
                .uri("/api/v1/jugadores/mercado?pagina=0&tamanio=1&busqueda={busqueda}", prefijo)
                .assertThat()
                .hasStatus(200)
                .bodyText()
                .contains("\"tamanioPagina\":1")
                .contains("\"totalElementos\":2")
                .contains("\"totalPaginas\":2");
    }

    @Test
    void mercado_filtraPorPosicion() {
        String sufijo = UUID.randomUUID().toString();
        String nombreArquero = "Arquero Mercado " + sufijo;
        String bodyArquero =
                """
                {"nombre":"%s","club":"Club","posicion":"Arquero",\
                "fechaNacimiento":"1990-01-01","nacionalidad":"Argentina",\
                "liga":"Liga de prueba"}"""
                        .formatted(nombreArquero);
        crear(tokenAdmin(), bodyArquero);
        crearYObtenerId("Delantero Mercado " + sufijo);

        mvc.get()
                .uri(
                        "/api/v1/jugadores/mercado?posicion=Arquero&busqueda={busqueda}",
                        "Mercado " + sufijo)
                .assertThat()
                .hasStatus(200)
                .bodyText()
                .contains(nombreArquero)
                .doesNotContain("Delantero Mercado " + sufijo);
    }

    // ---- GET /jugadores/{id}/cotizaciones (UC-08, público) ----

    @Test
    void obtenerCotizaciones_sinAutenticacion_devuelveListaVaciaParaUnJugadorSinCalculoPrevio() {
        String id = crearYObtenerId("Jugador Sin Cotizaciones " + UUID.randomUUID());

        mvc.get()
                .uri("/api/v1/jugadores/{id}/cotizaciones", id)
                .assertThat()
                .hasStatus(200)
                .bodyText()
                .isEqualTo("[]");
    }

    @Test
    void obtenerCotizaciones_conJugadorInexistente_responde404() {
        mvc.get()
                .uri("/api/v1/jugadores/{id}/cotizaciones", UUID.randomUUID())
                .assertThat()
                .hasStatus(404)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void obtenerCotizaciones_conEstrategia_responde200() {
        String id = crearYObtenerId("Jugador Cotizaciones Estrategia " + UUID.randomUUID());
        mvc.get()
                .uri("/api/v1/jugadores/{id}/cotizaciones?estrategia=RENDIMIENTO_ACTUAL", id)
                .assertThat()
                .hasStatus(200);
    }

    // ---- GET /jugadores/{id}/cotizaciones/{semana} (público) ----

    private void guardarCotizacion(
            String jugadorId,
            String semana,
            String valor,
            EstrategiaCotizacion estrategia,
            Instant fechaCalculo) {
        cotizacionHistoricaRepository.save(
                CotizacionHistoricaEntity.desde(
                        CotizacionHistorica.calcular(
                                UUID.fromString(jugadorId),
                                semana,
                                new BigDecimal(valor),
                                OrigenCotizacion.MANUAL,
                                estrategia,
                                fechaCalculo)));
    }

    @Test
    void obtenerCotizacionDeSemana_conVariasEnLaSemana_devuelveLaUltima() {
        String id = crearYObtenerId("Jugador Semana " + UUID.randomUUID());
        Instant base = Instant.parse("2026-10-05T03:00:00Z");
        guardarCotizacion(id, "2026-W40", "101.00", EstrategiaCotizacion.RENDIMIENTO_ACTUAL, base);
        guardarCotizacion(
                id,
                "2026-W40",
                "103.50",
                EstrategiaCotizacion.RENDIMIENTO_ACTUAL,
                base.plusSeconds(3600));
        guardarCotizacion(
                id,
                "2026-W41",
                "99.00",
                EstrategiaCotizacion.RENDIMIENTO_ACTUAL,
                base.plusSeconds(7 * 86400));

        mvc.get()
                .uri("/api/v1/jugadores/{id}/cotizaciones/2026-W40", id)
                .assertThat()
                .hasStatus(200)
                .bodyText()
                .contains("\"semana\":\"2026-W40\"")
                .contains("\"valor\":103.50");
    }

    @Test
    void obtenerCotizacionDeSemana_conEstrategia_filtraPorEsaEstrategia() {
        String id = crearYObtenerId("Jugador Semana Estrategia " + UUID.randomUUID());
        Instant base = Instant.parse("2026-10-05T03:00:00Z");
        guardarCotizacion(
                id, "2026-W40", "95.00", EstrategiaCotizacion.RENDIMIENTO_TEMPORADA, base);
        guardarCotizacion(
                id,
                "2026-W40",
                "110.00",
                EstrategiaCotizacion.RENDIMIENTO_ACTUAL,
                base.plusSeconds(3600));

        mvc.get()
                .uri(
                        "/api/v1/jugadores/{id}/cotizaciones/2026-W40?estrategia=RENDIMIENTO_TEMPORADA",
                        id)
                .assertThat()
                .hasStatus(200)
                .bodyText()
                .contains("\"valor\":95.00")
                .contains("\"estrategia\":\"RENDIMIENTO_TEMPORADA\"");
    }

    @Test
    void obtenerCotizacionDeSemana_sinCotizacionEnEsaSemana_responde404() {
        String id = crearYObtenerId("Jugador Semana Vacia " + UUID.randomUUID());

        mvc.get()
                .uri("/api/v1/jugadores/{id}/cotizaciones/2026-W40", id)
                .assertThat()
                .hasStatus(404)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void obtenerCotizacionDeSemana_conJugadorInexistente_responde404() {
        mvc.get()
                .uri("/api/v1/jugadores/{id}/cotizaciones/2026-W40", UUID.randomUUID())
                .assertThat()
                .hasStatus(404);
    }

    @Test
    void obtenerCotizacionDeSemana_conSemanaMalFormada_responde400() {
        String id = crearYObtenerId("Jugador Semana Formato " + UUID.randomUUID());

        mvc.get()
                .uri("/api/v1/jugadores/{id}/cotizaciones/2026-44", id)
                .assertThat()
                .hasStatus(400);
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
                "fechaNacimiento":"1987-06-24","nacionalidad":"Argentina",\
                "liga":"Liga de prueba"}""";

        crear(tokenAdmin(), body).assertThat().hasStatus(400);
    }

    // ---- PUT /jugadores/{id} (UC-04, ADMIN) ----

    @Test
    void editar_conTokenAdmin_actualizaDatosSinTocarEstadoNiTokensDisponibles() {
        String id = crearYObtenerId("Nombre Original " + UUID.randomUUID());
        String bodyEdicion =
                """
                {"nombre":"Nombre Editado","club":"Club Editado","posicion":"Mediocampista",\
                "fechaNacimiento":"1990-05-10","nacionalidad":"Brasil",\
                "liga":"Brasileirao"}""";

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

    // ---- POST /jugadores/{id}/baja (UC-15, ADMIN, tasks.md T6.6) ----

    private Usuario crearUsuarioConSaldo(BigDecimal saldo) {
        Usuario usuario =
                usuarioService.registrar(
                        "test-" + UUID.randomUUID() + "@example.com", "hash-bcrypt");
        if (saldo.signum() > 0) {
            usuarioService.recargarSaldo(usuario.getId(), saldo, UUID.randomUUID());
        }
        return usuario;
    }

    private String tokenDe(Usuario usuario) {
        return jwtService.generarToken(usuario.getId(), usuario.getEmail(), usuario.getRol());
    }

    @Test
    void darDeBaja_sinToken_responde401() {
        String id = crearYObtenerId("Jugador Baja Sin Token " + UUID.randomUUID());

        mvc.post().uri("/api/v1/jugadores/{id}/baja", id).assertThat().hasStatus(401);
    }

    @Test
    void darDeBaja_conTokenUser_responde403() {
        String id = crearYObtenerId("Jugador Baja Rol User " + UUID.randomUUID());

        mvc.post()
                .uri("/api/v1/jugadores/{id}/baja", id)
                .header("Authorization", "Bearer " + tokenUser())
                .assertThat()
                .hasStatus(403);
    }

    @Test
    void darDeBaja_conJugadorInexistente_responde404() {
        mvc.post()
                .uri("/api/v1/jugadores/{id}/baja", UUID.randomUUID())
                .header("Authorization", "Bearer " + tokenAdmin())
                .assertThat()
                .hasStatus(404)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void darDeBaja_conTokenAdmin_marcaInactivoYResponde200() {
        String id = crearYObtenerId("Jugador Baja Admin " + UUID.randomUUID());

        mvc.post()
                .uri("/api/v1/jugadores/{id}/baja", id)
                .header("Authorization", "Bearer " + tokenAdmin())
                .assertThat()
                .hasStatus(200)
                .bodyText()
                .contains("\"estado\":\"INACTIVO\"");

        mvc.get()
                .uri("/api/v1/jugadores/{id}", id)
                .assertThat()
                .hasStatus(200)
                .bodyText()
                .contains("\"estado\":\"INACTIVO\"")
                .contains("\"tokensDisponibles\":0");
    }

    @Test
    void darDeBaja_conTenenciaYOfertaAbierta_compensaAAmbosUsuariosYCancelaLaOferta() {
        String id = crearYObtenerId("Jugador Baja Con Ofertas " + UUID.randomUUID());
        Usuario comprador = crearUsuarioConSaldo(new BigDecimal("1000.00"));
        Usuario vendedor = crearUsuarioConSaldo(new BigDecimal("1000.00"));

        // comprador compra 10 al sistema; vendedor compra 10 y publica 6 en una oferta P2P.
        mvc.post()
                .uri("/api/v1/jugadores/{id}/compras", id)
                .header("Authorization", "Bearer " + tokenDe(comprador))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":10}")
                .assertThat()
                .hasStatus(201);
        mvc.post()
                .uri("/api/v1/jugadores/{id}/compras", id)
                .header("Authorization", "Bearer " + tokenDe(vendedor))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\":10}")
                .assertThat()
                .hasStatus(201);
        mvc.post()
                .uri("/api/v1/ofertas")
                .header("Authorization", "Bearer " + tokenDe(vendedor))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        "{\"jugadorId\":\"%s\",\"cantidad\":6,\"precioUnitario\":20.00,\"horasLimite\":24}"
                                .formatted(id))
                .assertThat()
                .hasStatus(201);

        mvc.post()
                .uri("/api/v1/jugadores/{id}/baja", id)
                .header("Authorization", "Bearer " + tokenAdmin())
                .assertThat()
                .hasStatus(200)
                .bodyText()
                .contains("\"estado\":\"INACTIVO\"");

        // Ambos quedan compensados por la totalidad de su tenencia (comprador: 10, vendedor: 10
        // incluidos los 6 que estaban reservados en la oferta) a la cotización inicial (100.00,
        // sin cálculo previo), y la oferta queda cancelada.
        mvc.get()
                .uri("/api/v1/movimientos/me")
                .header("Authorization", "Bearer " + tokenDe(comprador))
                .assertThat()
                .bodyText()
                .contains("\"tipo\":\"COMPENSACION_BAJA\"")
                .contains("\"cantidad\":10");
        mvc.get()
                .uri("/api/v1/movimientos/me")
                .header("Authorization", "Bearer " + tokenDe(vendedor))
                .assertThat()
                .bodyText()
                .contains("\"tipo\":\"COMPENSACION_BAJA\"")
                .contains("\"cantidad\":10");
        mvc.get().uri("/api/v1/ofertas?jugadorId={id}", id).assertThat().bodyText().isEqualTo("[]");
    }

    // ---- GET /jugadores/{id}/estadisticas (UC-07) ----

    @Test
    void obtenerEstadisticas_conJugadorInexistente_responde404() {
        mvc.get()
                .uri("/api/v1/jugadores/{id}/estadisticas", UUID.randomUUID())
                .assertThat()
                .hasStatus(404);
    }

    @Test
    void obtenerEstadisticas_sinSnapshotDeRendimiento_responde204() {
        String id = crearYObtenerId("Sin Estadisticas");

        mvc.get().uri("/api/v1/jugadores/{id}/estadisticas", id).assertThat().hasStatus(204);
    }

    @Test
    void obtenerEstadisticas_conSnapshot_esPublicoYDevuelveElAcumuladoDeTemporada() {
        String id = crearYObtenerId("Con Estadisticas");
        rendimientoAcumuladoWhoScoredRepository.save(
                RendimientoAcumuladoWhoScoredEntity.desde(
                        RendimientoAcumuladoWhoScored.primeraVez(
                                UUID.fromString(id),
                                """
                                {"apps":9,"minsPlayed":503,"goals":2,"assists":3,\
                                "manOfTheMatch":0,"yellowCard":1,"redCard":0}""")));

        mvc.get()
                .uri("/api/v1/jugadores/{id}/estadisticas", id)
                .assertThat()
                .hasStatus(200)
                .bodyText()
                .contains("\"partidosJugados\":9")
                .contains("\"minutosJugados\":503")
                .contains("\"goles\":2")
                .contains("\"asistencias\":3")
                .contains("\"tarjetasAmarillas\":1");
    }
}
