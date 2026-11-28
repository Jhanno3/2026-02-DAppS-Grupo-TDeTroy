package com.tdetroy.valuacion.repositories.rendimiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import com.sun.net.httpserver.HttpServer;
import com.tdetroy.valuacion.config.WhoScoredLigaProperties;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * Test de {@link WhoScoredLigaRepositoryImpl} contra fixtures propios (constitution.md §4), nunca
 * contra el sitio real ni contra el sidecar de Playwright. El {@link
 * WhoScoredLigaRepositoryImpl.PaginasFetcher} inyectado en el constructor paquete-privado reemplaza
 * por completo el recorrido de páginas que en producción hace {@code scraper-service}.
 */
class WhoScoredLigaRepositoryImplTest {

    private static final String LIGA = "La Liga";
    private static final String CLAVE_LIGA = "la-liga";
    private static final String URL_LIGA = "https://whoscored.test/estadisticas/la-liga";
    private static final String IMAGENES_URL = "https://imagenes.whoscored.test/img";

    @Test
    void obtenerAcumuladoLiga_recorreTodasLasPaginasYTraduceCadaFila() {
        WhoScoredLigaRepositoryImpl repository =
                repositorio(
                        Map.of(CLAVE_LIGA, URL_LIGA),
                        url -> {
                            assertThat(url).isEqualTo(URL_LIGA);
                            return List.of(
                                    documentoDesdeFixture("fixtures/whoscored/liga-pagina-1.html"),
                                    documentoDesdeFixture("fixtures/whoscored/liga-pagina-2.html"));
                        });

        List<JugadorRendimientoLigaCrudo> resultado = repository.obtenerAcumuladoLiga(LIGA);

        assertThat(resultado).hasSize(3);

        JugadorRendimientoLigaCrudo primero = resultado.get(0);
        assertThat(primero.nombre()).isEqualTo("Kylian Mbappé");
        assertThat(primero.club()).isEqualTo("Real Madrid CF");
        assertThat(primero.metricas())
                .containsOnly(
                        entry("apps", 10),
                        entry("goals", 8),
                        entry("assists", 3),
                        entry("manOfTheMatch", 2),
                        entry("yellowCard", 1),
                        entry("redCard", 0),
                        entry("minsPlayed", 880));
        // El id de WhoScored sale del link "/players/1/show/..." de la misma fila.
        assertThat(primero.fotoUrl()).isEqualTo(IMAGENES_URL + "/players/1.jpg");

        // "8(1)" (titular + suplente) en la celda de apariciones -> suma 9.
        JugadorRendimientoLigaCrudo segundo = resultado.get(1);
        assertThat(segundo.nombre()).isEqualTo("Jude Bellingham");
        assertThat(segundo.club()).isEqualTo("Real Madrid CF");
        assertThat(segundo.metricas()).containsEntry("apps", 9).containsEntry("yellowCard", 2);

        // "-" en amarillas/rojas de WhoScored significa 0, no un valor ausente.
        JugadorRendimientoLigaCrudo tercero = resultado.get(2);
        assertThat(tercero.nombre()).isEqualTo("Robert Lewandowski");
        assertThat(tercero.club()).isEqualTo("FC Barcelona");
        assertThat(tercero.metricas())
                .containsEntry("apps", 11)
                .containsEntry("yellowCard", 0)
                .containsEntry("redCard", 0);
    }

    @Test
    void obtenerAcumuladoLiga_normalizaElNombreDeLigaParaBuscarLaUrl() {
        WhoScoredLigaRepositoryImpl repository =
                repositorio(
                        Map.of(CLAVE_LIGA, URL_LIGA),
                        url -> {
                            assertThat(url).isEqualTo(URL_LIGA);
                            return List.of();
                        });

        // Acentos/mayusculas/espacios distintos a la clave de config, pero deben resolver igual.
        repository.obtenerAcumuladoLiga("LA LIGA");
    }

    @Test
    void obtenerAcumuladoLiga_sinUrlConfigurada_devuelveListaVacia() {
        WhoScoredLigaRepositoryImpl repository =
                repositorio(
                        Map.of(),
                        url ->
                                List.of(
                                        documentoDesdeFixture(
                                                "fixtures/whoscored/liga-pagina-1.html")));

        List<JugadorRendimientoLigaCrudo> resultado =
                repository.obtenerAcumuladoLiga("Premier League");

        assertThat(resultado).isEmpty();
    }

    @Test
    void obtenerAcumuladoLiga_fallaDeRedAlPaginar_seAislaYDevuelveListaVacia() {
        WhoScoredLigaRepositoryImpl repository =
                repositorio(
                        Map.of(CLAVE_LIGA, URL_LIGA),
                        url -> {
                            throw new IOException("sidecar no disponible");
                        });

        assertThat(repository.obtenerAcumuladoLiga(LIGA)).isEmpty();
    }

    @Test
    void obtenerAcumuladoLiga_cambioDeEstructuraDelSitio_seAislaYDevuelveListaVacia() {
        WhoScoredLigaRepositoryImpl repository =
                repositorio(
                        Map.of(CLAVE_LIGA, URL_LIGA),
                        url ->
                                List.of(
                                        Jsoup.parse(
                                                "<table><tr class=\"otra-cosa\"></tr></table>")));

        assertThat(repository.obtenerAcumuladoLiga(LIGA)).isEmpty();
    }

    @Test
    void obtenerAcumuladoLiga_filaSinIdDeWhoScoredEnElLink_quedaSinFoto() {
        String fila =
                "<table><tbody id=\"player-table-statistics-body\"><tr><td></td>"
                        + "<td class=\"grid-ghost-cell\"><a class=\"player-link\" href=\"/otra-cosa\">"
                        + "Jugador Sin Id</a><a class=\"player-meta-data\"><span class=\"team-name\">"
                        + "Club, </span></a></td><td>3</td></tr></tbody></table>";
        WhoScoredLigaRepositoryImpl repository =
                repositorio(Map.of(CLAVE_LIGA, URL_LIGA), url -> List.of(Jsoup.parse(fila)));

        List<JugadorRendimientoLigaCrudo> resultado = repository.obtenerAcumuladoLiga(LIGA);

        assertThat(resultado).singleElement().satisfies(c -> assertThat(c.fotoUrl()).isNull());
    }

    @Test
    void readTimeout_aislaElFalloYDevuelveListaVaciaSinEsperarDeMas() throws IOException {
        CountDownLatch liberar = new CountDownLatch(1);
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext(
                "/",
                exchange -> {
                    try {
                        liberar.await(3, TimeUnit.SECONDS);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                    exchange.close();
                });
        servidor.start();
        try {
            String fetcherUrl =
                    "http://127.0.0.1:" + servidor.getAddress().getPort() + "/fetch-liga";
            WhoScoredLigaProperties propiedades = new WhoScoredLigaProperties();
            propiedades.setLigaUrls(Map.of(CLAVE_LIGA, URL_LIGA));
            WhoScoredLigaRepositoryImpl repoConTimeout =
                    new WhoScoredLigaRepositoryImpl(
                            RestClient.builder(),
                            propiedades,
                            fetcherUrl,
                            IMAGENES_URL,
                            200L,
                            100L);

            Instant inicio = Instant.now();
            List<JugadorRendimientoLigaCrudo> resultado = repoConTimeout.obtenerAcumuladoLiga(LIGA);
            Duration duracion = Duration.between(inicio, Instant.now());

            assertThat(resultado).isEmpty();
            assertThat(duracion).isLessThan(Duration.ofSeconds(2));
        } finally {
            liberar.countDown();
            servidor.stop(0);
        }
    }

    private static WhoScoredLigaRepositoryImpl repositorio(
            Map<String, String> urlsPorLiga, WhoScoredLigaRepositoryImpl.PaginasFetcher fetcher) {
        return new WhoScoredLigaRepositoryImpl(urlsPorLiga, fetcher, IMAGENES_URL);
    }

    private static Document documentoDesdeFixture(String ruta) {
        try (InputStream in =
                WhoScoredLigaRepositoryImplTest.class.getClassLoader().getResourceAsStream(ruta)) {
            return Jsoup.parse(in, "UTF-8", URL_LIGA);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
