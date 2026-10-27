package com.tdetroy.valuacion.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
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
    void obtenerFotoUrl_cambioDeEstructuraDelSitio_seAislaYDevuelveVacio() {
        WhoScoredRepositoryImpl repository =
                new WhoScoredRepositoryImpl(BASE_URL, url -> Jsoup.parse("<html></html>"));

        assertThat(repository.obtenerFotoUrl(UUID.randomUUID())).isEmpty();
    }

    @Test
    void obtenerFotoUrl_jugadorIdNulo_lanzaExcepcion() {
        WhoScoredRepositoryImpl repository =
                new WhoScoredRepositoryImpl(BASE_URL, url -> Jsoup.parse("<html></html>"));

        assertThatNullPointerException().isThrownBy(() -> repository.obtenerFotoUrl(null));
    }

    private static Document documentoDesdeFixture(String ruta) {
        try (InputStream in =
                WhoScoredRepositoryImplTest.class.getClassLoader().getResourceAsStream(ruta)) {
            return Jsoup.parse(in, "UTF-8", BASE_URL);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
