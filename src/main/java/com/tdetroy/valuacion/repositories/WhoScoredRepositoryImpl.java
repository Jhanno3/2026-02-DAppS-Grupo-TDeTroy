package com.tdetroy.valuacion.repositories;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * {@inheritDoc}
 *
 * <p>Scrapea la página pública de perfil de un jugador en WhoScored (constitution §1: fuente no
 * oficial, intrínsecamente frágil ante cambios de estructura del sitio) leyendo el {@code src} de
 * {@code img#foto-jugador}; devuelve {@link Optional#empty()} si la página no trae esa foto.
 *
 * <p>Las métricas de rendimiento en sí ya no se scrapean por jugador desde esta clase: esa
 * responsabilidad pasó a {@link WhoScoredLigaRepositoryImpl} (ingesta por liga completa, plan.md
 * §8.1) — esta clase quedó acotada a la foto de perfil, que la página de liga no provee.
 *
 * <p>Aísla sus propios errores: cualquier falla de red/parseo se loguea acá y devuelve {@link
 * Optional#empty()} en lugar de propagar la excepción — una caída o un cambio de estructura de
 * WhoScored nunca debe tumbar el resto del sistema (constitution §1, "Manejo explícito de
 * fallos/indisponibilidad por fuente").
 */
@Repository
public class WhoScoredRepositoryImpl implements RendimientoExternoRepository {

    private static final Logger log = LoggerFactory.getLogger(WhoScoredRepositoryImpl.class);

    private static final String USER_AGENT = "Mozilla/5.0 (compatible; ValuacionJugadoresBot/1.0)";
    private static final int TIMEOUT_MS = 35_000;
    private static final String SELECTOR_FOTO = "img#foto-jugador";

    private final String baseUrl;
    private final DocumentoFetcher documentoFetcher;

    @Autowired
    public WhoScoredRepositoryImpl(
            @Value("${app.whoscored.base-url}") String baseUrl,
            @Value("${app.whoscored.fetcher-url:}") String fetcherUrl) {
        this(
                baseUrl,
                url -> {
                    String urlEfectiva = url;
                    // Si tenemos configurado el scraper sidecar, pasamos la petición por él
                    if (fetcherUrl != null && !fetcherUrl.isBlank()) {
                        String urlCodificada =
                                java.net.URLEncoder.encode(
                                        url, java.nio.charset.StandardCharsets.UTF_8);
                        urlEfectiva = "%s?url=%s".formatted(fetcherUrl, urlCodificada);
                    }
                    return Jsoup.connect(urlEfectiva)
                            .userAgent(USER_AGENT)
                            .timeout(TIMEOUT_MS)
                            .maxBodySize(0)
                            .get();
                });
    }

    WhoScoredRepositoryImpl(String baseUrl, DocumentoFetcher documentoFetcher) {
        this.baseUrl = baseUrl;
        this.documentoFetcher = documentoFetcher;
    }

    @Override
    public Optional<String> obtenerFotoUrl(UUID jugadorId) {
        Objects.requireNonNull(jugadorId, "jugadorId no puede ser null");
        String url = "%s/jugadores/%s/perfil".formatted(baseUrl, jugadorId);
        try {
            Element foto = documentoFetcher.obtener(url).selectFirst(SELECTOR_FOTO);
            if (foto == null) {
                return Optional.empty();
            }
            String src = foto.attr("src");
            return src.isBlank() ? Optional.empty() : Optional.of(src);
        } catch (IOException | RuntimeException ex) {
            log.error(
                    "Fallo al obtener la foto de WhoScored para el jugador {}: {}",
                    jugadorId,
                    ex.getMessage(),
                    ex);
            return Optional.empty();
        }
    }

    /** Extremo de infraestructura inyectable para no depender de red real en los tests. */
    @FunctionalInterface
    interface DocumentoFetcher {
        Document obtener(String url) throws IOException;
    }
}
