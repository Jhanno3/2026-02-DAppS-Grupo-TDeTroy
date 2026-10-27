package com.tdetroy.valuacion.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Test del constructor {@code @Autowired} de {@link WhoScoredRepositoryImpl}: es el único que arma
 * {@code urlEfectiva} (ruteo opcional por el sidecar {@code app.whoscored.fetcher-url}), algo que
 * {@link WhoScoredRepositoryImplTest} no ejercita porque reemplaza el {@link
 * WhoScoredRepositoryImpl.DocumentoFetcher} entero via el constructor paquete-privado. Ejercita
 * este mecanismo a través de {@code obtenerFotoUrl} (el único método que le queda a esta clase) —
 * el armado de {@code urlEfectiva} es el mismo sin importar qué método lo dispare. Usa un {@link
 * HttpServer} embebido en localhost (sin red real, constitution.md §4) para verificar qué URL arma
 * Jsoup realmente en cada caso.
 */
class WhoScoredRepositoryImplFetcherTest {

    private HttpServer server;

    @AfterEach
    void detenerServidor() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sinFetcherConfigurado_consultaDirectamenteLaUrlOriginal() throws IOException {
        AtomicReference<String> rutaRecibida = new AtomicReference<>();
        server = iniciarServidor("/jugadores", exchange -> responder(exchange, rutaRecibida));
        String baseUrl = "http://localhost:%d".formatted(server.getAddress().getPort());

        WhoScoredRepositoryImpl repository = new WhoScoredRepositoryImpl(baseUrl, "");

        UUID jugadorId = UUID.randomUUID();
        repository.obtenerFotoUrl(jugadorId);

        assertThat(rutaRecibida.get()).isEqualTo("/jugadores/%s/perfil".formatted(jugadorId));
    }

    @Test
    void conFetcherEnBlanco_consultaDirectamenteLaUrlOriginal() throws IOException {
        AtomicReference<String> rutaRecibida = new AtomicReference<>();
        server = iniciarServidor("/jugadores", exchange -> responder(exchange, rutaRecibida));
        String baseUrl = "http://localhost:%d".formatted(server.getAddress().getPort());

        WhoScoredRepositoryImpl repository = new WhoScoredRepositoryImpl(baseUrl, "   ");

        UUID jugadorId = UUID.randomUUID();
        repository.obtenerFotoUrl(jugadorId);

        assertThat(rutaRecibida.get()).isEqualTo("/jugadores/%s/perfil".formatted(jugadorId));
    }

    @Test
    void conFetcherNulo_consultaDirectamenteLaUrlOriginal() throws IOException {
        AtomicReference<String> rutaRecibida = new AtomicReference<>();
        server = iniciarServidor("/jugadores", exchange -> responder(exchange, rutaRecibida));
        String baseUrl = "http://localhost:%d".formatted(server.getAddress().getPort());

        WhoScoredRepositoryImpl repository = new WhoScoredRepositoryImpl(baseUrl, (String) null);

        UUID jugadorId = UUID.randomUUID();
        repository.obtenerFotoUrl(jugadorId);

        assertThat(rutaRecibida.get()).isEqualTo("/jugadores/%s/perfil".formatted(jugadorId));
    }

    @Test
    void conFetcherConfigurado_enrutaLaPeticionPorElSidecarConLaUrlCodificada() throws IOException {
        AtomicReference<String> rutaRecibida = new AtomicReference<>();
        server = iniciarServidor("/fetch", exchange -> responder(exchange, rutaRecibida));
        String baseUrl = "http://localhost:%d".formatted(server.getAddress().getPort());
        String fetcherUrl = "%s/fetch".formatted(baseUrl);

        WhoScoredRepositoryImpl repository = new WhoScoredRepositoryImpl(baseUrl, fetcherUrl);

        UUID jugadorId = UUID.randomUUID();
        repository.obtenerFotoUrl(jugadorId);

        String urlOriginalEsperada = "%s/jugadores/%s/perfil".formatted(baseUrl, jugadorId);
        assertThat(rutaRecibida.get()).startsWith("/fetch?url=");
        String queryCodificada = rutaRecibida.get().substring("/fetch?url=".length());
        assertThat(URLDecoder.decode(queryCodificada, StandardCharsets.UTF_8))
                .isEqualTo(urlOriginalEsperada);
    }

    private static HttpServer iniciarServidor(String contexto, HttpHandler handler)
            throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext(contexto, handler);
        servidor.start();
        return servidor;
    }

    private static void responder(HttpExchange exchange, AtomicReference<String> capturaRuta)
            throws IOException {
        capturaRuta.set(exchange.getRequestURI().getRawPath() + queryOrEmpty(exchange));
        byte[] cuerpo = "<html></html>".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, cuerpo.length);
        try (var out = exchange.getResponseBody()) {
            out.write(cuerpo);
        }
    }

    private static String queryOrEmpty(HttpExchange exchange) {
        String query = exchange.getRequestURI().getRawQuery();
        return query == null ? "" : "?" + query;
    }
}
