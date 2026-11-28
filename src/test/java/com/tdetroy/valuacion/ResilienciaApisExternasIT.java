package com.tdetroy.valuacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.assertj.MockMvcTester.create;

import com.sun.net.httpserver.HttpServer;
import com.tdetroy.valuacion.config.seguridad.JwtService;
import com.tdetroy.valuacion.model.usuario.RolUsuario;
import com.tdetroy.valuacion.services.rendimiento.RendimientoService;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifica que la aplicación sigue operando con normalidad cuando las APIs externas
 * (Football-Data.org y WhoScored) no responden o exceden el timeout configurado: las ingestas
 * aíslan los fallos sin propagar excepciones y los endpoints de catálogo, mercado y ranking
 * continúan respondiendo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ResilienciaApisExternasIT {

    private static HttpServer servidorLento;
    private static CountDownLatch liberarServidor;
    private static final AtomicInteger llamadasExternas = new AtomicInteger();

    @Autowired private RendimientoService rendimientoService;
    @Autowired private JwtService jwtService;
    private MockMvcTester mvc;

    @Autowired
    void inicializarMvcTester(MockMvc mockMvc) {
        this.mvc = create(mockMvc);
    }

    @BeforeAll
    static void iniciarServidorLento() throws IOException {
        liberarServidor = new CountDownLatch(1);
        servidorLento = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidorLento.createContext(
                "/",
                exchange -> {
                    llamadasExternas.incrementAndGet();
                    try {
                        liberarServidor.await(3, TimeUnit.SECONDS);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                    exchange.close();
                });
        servidorLento.start();
    }

    @AfterAll
    static void detenerServidorLento() {
        if (liberarServidor != null) {
            liberarServidor.countDown();
        }
        if (servidorLento != null) {
            servidorLento.stop(0);
        }
    }

    @DynamicPropertySource
    static void configurarApisExternasLentas(DynamicPropertyRegistry registry) {
        int port = servidorLento.getAddress().getPort();
        String baseUrl = "http://127.0.0.1:" + port;
        registry.add("app.football-data.base-url", () -> baseUrl);
        registry.add("app.football-data.connect-timeout-ms", () -> "200");
        registry.add("app.football-data.read-timeout-ms", () -> "100");
        registry.add("app.whoscored.fetcher-liga-url", () -> baseUrl + "/fetch-liga");
        registry.add("app.whoscored.connect-timeout-ms", () -> "200");
        registry.add("app.whoscored.read-timeout-ms", () -> "100");
    }

    @Test
    void sistemaSigueOperandoCuandoApisExternasExcedenTimeout() {
        String tokenAdmin =
                jwtService.generarToken(UUID.randomUUID(), "admin@example.com", RolUsuario.ADMIN);
        assertThat(
                        mvc.post()
                                .uri("/api/v1/jugadores")
                                .header("Authorization", "Bearer " + tokenAdmin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"nombre":"Pedri","club":"FC Barcelona","posicion":"Mediocampista",\
                                        "fechaNacimiento":"2002-11-25","nacionalidad":"España","liga":"La Liga"}
                                        """))
                .hasStatus(201);

        int llamadasAntes = llamadasExternas.get();
        rendimientoService.ingestarFixtures();
        rendimientoService.ingestarRendimiento();
        assertThat(llamadasExternas.get()).isGreaterThanOrEqualTo(llamadasAntes + 2);

        assertThat(mvc.get().uri("/api/v1/jugadores")).hasStatusOk();
        assertThat(mvc.get().uri("/api/v1/jugadores/mercado")).hasStatusOk();
        assertThat(mvc.get().uri("/api/v1/jugadores/ranking")).hasStatusOk();
    }
}
