package com.tdetroy.valuacion.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

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

    @Test
    void obtenerAcumuladoLiga_recorreTodasLasPaginasYTraduceCadaFila() {
        WhoScoredLigaRepositoryImpl repository =
                new WhoScoredLigaRepositoryImpl(
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
                new WhoScoredLigaRepositoryImpl(
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
                new WhoScoredLigaRepositoryImpl(
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
                new WhoScoredLigaRepositoryImpl(
                        Map.of(CLAVE_LIGA, URL_LIGA),
                        url -> {
                            throw new IOException("sidecar no disponible");
                        });

        assertThat(repository.obtenerAcumuladoLiga(LIGA)).isEmpty();
    }

    @Test
    void obtenerAcumuladoLiga_cambioDeEstructuraDelSitio_seAislaYDevuelveListaVacia() {
        WhoScoredLigaRepositoryImpl repository =
                new WhoScoredLigaRepositoryImpl(
                        Map.of(CLAVE_LIGA, URL_LIGA),
                        url ->
                                List.of(
                                        Jsoup.parse(
                                                "<table><tr class=\"otra-cosa\"></tr></table>")));

        assertThat(repository.obtenerAcumuladoLiga(LIGA)).isEmpty();
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
