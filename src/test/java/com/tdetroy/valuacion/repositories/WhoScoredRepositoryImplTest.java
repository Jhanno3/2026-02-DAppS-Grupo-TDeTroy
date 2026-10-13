package com.tdetroy.valuacion.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.entry;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

/**
 * Test de {@link WhoScoredRepositoryImpl} contra fixtures propios (constitution.md §4: "prohibido
 * que un test unitario dependa de red real... los adapters de WhoScored y Football-Data.org no
 * deben depender de la disponibilidad real de esos sitios/APIs"), nunca contra el sitio real. El
 * {@link WhoScoredRepositoryImpl.DocumentoFetcher} inyectado en el constructor paquete-privado
 * reemplaza la conexión HTTP real de Jsoup, mismo rol que {@link
 * org.springframework.test.web.client.MockRestServiceServer} para {@link
 * FootballDataRepositoryImpl}.
 */
class WhoScoredRepositoryImplTest {

    private static final String BASE_URL = "https://whoscored.test";

    @Test
    void rendimientoEnRango_seTraduceARendimientoCrudo() {
        UUID jugadorId = UUID.randomUUID();
        LocalDate desde = LocalDate.of(2026, 2, 9);
        LocalDate hasta = LocalDate.of(2026, 2, 23);
        String urlEsperada =
                "%s/jugadores/%s/rendimiento?desde=%s&hasta=%s"
                        .formatted(BASE_URL, jugadorId, desde, hasta);

        WhoScoredRepositoryImpl repository =
                new WhoScoredRepositoryImpl(
                        BASE_URL,
                        url -> {
                            assertThat(url).isEqualTo(urlEsperada);
                            return documentoDesdeFixture();
                        });

        List<RendimientoCrudo> rendimientos =
                repository.obtenerRendimiento(jugadorId, desde, hasta);

        assertThat(rendimientos).hasSize(2);

        RendimientoCrudo primero = rendimientos.get(0);
        assertThat(primero.partidoExternoId()).isEqualTo("55501");
        assertThat(primero.fechaPartido()).isEqualTo(LocalDate.of(2026, 2, 15));
        assertThat(primero.metricas())
                .containsEntry("rating", 7.8)
                .containsEntry("goles", 1)
                .containsEntry("asistencias", 0)
                .containsEntry("pasesCompletadosPorc", 91.5)
                .containsEntry("minutosJugados", 90);

        RendimientoCrudo segundo = rendimientos.get(1);
        assertThat(segundo.partidoExternoId()).isEqualTo("55502");
        assertThat(segundo.fechaPartido()).isEqualTo(LocalDate.of(2026, 2, 22));
        assertThat(segundo.metricas()).containsOnly(entry("rating", 6.9), entry("goles", 0));
    }

    @Test
    void fallaDeRedAlScrapear_seAislaYDevuelveListaVacia() {
        WhoScoredRepositoryImpl repository =
                new WhoScoredRepositoryImpl(
                        BASE_URL,
                        url -> {
                            throw new IOException("sitio no disponible");
                        });

        List<RendimientoCrudo> rendimientos =
                repository.obtenerRendimiento(
                        UUID.randomUUID(), LocalDate.of(2026, 2, 9), LocalDate.of(2026, 2, 15));

        assertThat(rendimientos).isEmpty();
    }

    @Test
    void cambioDeEstructuraDelSitio_seAislaYDevuelveListaVacia() {
        WhoScoredRepositoryImpl repository =
                new WhoScoredRepositoryImpl(
                        BASE_URL,
                        url ->
                                Jsoup.parse(
                                        "<table><tr class=\"partido\""
                                                + " data-partido-id=\"1\"></tr></table>"));

        List<RendimientoCrudo> rendimientos =
                repository.obtenerRendimiento(
                        UUID.randomUUID(), LocalDate.of(2026, 2, 9), LocalDate.of(2026, 2, 15));

        assertThat(rendimientos).isEmpty();
    }

    @Test
    void sinFilasDePartido_devuelveListaVacia() {
        WhoScoredRepositoryImpl repository =
                new WhoScoredRepositoryImpl(BASE_URL, url -> Jsoup.parse("<html></html>"));

        List<RendimientoCrudo> rendimientos =
                repository.obtenerRendimiento(
                        UUID.randomUUID(), LocalDate.of(2026, 2, 9), LocalDate.of(2026, 2, 15));

        assertThat(rendimientos).isEmpty();
    }

    @Test
    void jugadorIdNulo_lanzaExcepcion() {
        WhoScoredRepositoryImpl repository = construirConFixtureVacia();
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                repository.obtenerRendimiento(
                                        null, LocalDate.of(2026, 2, 9), LocalDate.of(2026, 2, 15)));
    }

    @Test
    void desdeNulo_lanzaExcepcion() {
        WhoScoredRepositoryImpl repository = construirConFixtureVacia();
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                repository.obtenerRendimiento(
                                        UUID.randomUUID(), null, LocalDate.of(2026, 2, 15)));
    }

    @Test
    void hastaNulo_lanzaExcepcion() {
        WhoScoredRepositoryImpl repository = construirConFixtureVacia();
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                repository.obtenerRendimiento(
                                        UUID.randomUUID(), LocalDate.of(2026, 2, 9), null));
    }

    @Test
    void hastaAnteriorADesde_lanzaExcepcion() {
        WhoScoredRepositoryImpl repository = construirConFixtureVacia();
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                repository.obtenerRendimiento(
                                        UUID.randomUUID(),
                                        LocalDate.of(2026, 2, 15),
                                        LocalDate.of(2026, 2, 9)));
    }

    private static WhoScoredRepositoryImpl construirConFixtureVacia() {
        return new WhoScoredRepositoryImpl(BASE_URL, url -> Jsoup.parse("<html></html>"));
    }

    private static Document documentoDesdeFixture() {
        return documentoDesdeFixture("fixtures/whoscored/rendimiento-jugador.html");
    }

    private static Document documentoDesdeFixture(String ruta) {
        try (InputStream in =
                WhoScoredRepositoryImplTest.class.getClassLoader().getResourceAsStream(ruta)) {
            return Jsoup.parse(in, "UTF-8", BASE_URL);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    // ---- obtenerFotoUrl ----

    @Test
    void obtenerFotoUrl_conFotoPublicada_devuelveLaUrl() {
        UUID jugadorId = UUID.randomUUID();
        String urlEsperada = "%s/jugadores/%s/perfil".formatted(BASE_URL, jugadorId);

        WhoScoredRepositoryImpl repository =
                new WhoScoredRepositoryImpl(
                        BASE_URL,
                        url -> {
                            assertThat(url).isEqualTo(urlEsperada);
                            return documentoDesdeFixture("fixtures/whoscored/perfil-jugador.html");
                        });

        Optional<String> foto = repository.obtenerFotoUrl(jugadorId);

        assertThat(foto).contains("https://whoscored.test/fotos/12345.jpg");
    }

    @Test
    void obtenerFotoUrl_sinFotoEnElPerfil_devuelveVacio() {
        WhoScoredRepositoryImpl repository =
                new WhoScoredRepositoryImpl(
                        BASE_URL,
                        url ->
                                documentoDesdeFixture(
                                        "fixtures/whoscored/perfil-jugador-sin-foto.html"));

        Optional<String> foto = repository.obtenerFotoUrl(UUID.randomUUID());

        assertThat(foto).isEmpty();
    }

    @Test
    void obtenerFotoUrl_fallaDeRed_seAislaYDevuelveVacio() {
        WhoScoredRepositoryImpl repository =
                new WhoScoredRepositoryImpl(
                        BASE_URL,
                        url -> {
                            throw new IOException("sitio no disponible");
                        });

        assertThat(repository.obtenerFotoUrl(UUID.randomUUID())).isEmpty();
    }

    @Test
    void obtenerFotoUrl_jugadorIdNulo_lanzaExcepcion() {
        WhoScoredRepositoryImpl repository = construirConFixtureVacia();

        assertThatNullPointerException().isThrownBy(() -> repository.obtenerFotoUrl(null));
    }
}
