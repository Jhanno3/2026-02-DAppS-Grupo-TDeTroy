package com.tdetroy.valuacion.repositories;

import java.io.IOException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * <p>Scrapea la página pública de rendimiento por partido de un jugador en WhoScored (constitution
 * §1: fuente no oficial, intrínsecamente frágil ante cambios de estructura del sitio) y traduce
 * cada partido encontrado a {@link RendimientoCrudo} — {@code RendimientoService} (tasks.md T3.5)
 * nunca ve el HTML ni los nombres de atributo propios de esta página (constitution §1/§2).
 *
 * <p>{@code metricas} se arma leyendo genéricamente todos los {@code td[data-metrica]} de cada fila
 * de partido, sin una lista fija de claves conocidas — así una métrica nueva que WhoScored empiece
 * a exponer (o deje de exponer) no rompe la traducción (plan.md §2.3/§6.2, spec.md UC-05: "sin
 * curación previa").
 *
 * <p>{@link #obtenerFotoUrl} scrapea una página de perfil separada ({@code /jugadores/{id}/perfil},
 * distinta de la de rendimiento) leyendo el {@code src} de {@code img#foto-jugador}; devuelve
 * {@link Optional#empty()} si la página no trae esa foto, mismo criterio de "no bloquea el resto
 * del catálogo" que {@link #obtenerRendimiento}.
 *
 * <p>Aísla sus propios errores: cualquier falla de red/parseo se loguea acá y devuelve lista vacía
 * (o {@link Optional#empty()}) en lugar de propagar la excepción — una caída o un cambio de
 * estructura de WhoScored nunca debe tumbar el resto del sistema ni al flujo de Football-Data.org
 * (constitution §1, "Manejo explícito de fallos/indisponibilidad por fuente").
 */
@Repository
public class WhoScoredRepositoryImpl implements RendimientoExternoRepository {

    private static final Logger log = LoggerFactory.getLogger(WhoScoredRepositoryImpl.class);

    private static final String USER_AGENT = "Mozilla/5.0 (compatible; ValuacionJugadoresBot/1.0)";
    private static final int TIMEOUT_MS = 35_000;
    ;
    private static final String SELECTOR_FILA_PARTIDO = "tr.partido";
    private static final String SELECTOR_METRICA = "td[data-metrica]";
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
    public List<RendimientoCrudo> obtenerRendimiento(
            UUID jugadorId, LocalDate desde, LocalDate hasta) {
        validarParametros(jugadorId, desde, hasta);
        String url =
                "%s/jugadores/%s/rendimiento?desde=%s&hasta=%s"
                        .formatted(baseUrl, jugadorId, desde, hasta);
        try {
            return traducir(documentoFetcher.obtener(url));
        } catch (IOException | RuntimeException ex) {
            log.error(
                    "Fallo al scrapear WhoScored para el jugador {} en el rango {} a {}: {}",
                    jugadorId,
                    desde,
                    hasta,
                    ex.getMessage(),
                    ex);
            return List.of();
        }
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

    private static void validarParametros(UUID jugadorId, LocalDate desde, LocalDate hasta) {
        Objects.requireNonNull(jugadorId, "jugadorId no puede ser null");
        Objects.requireNonNull(desde, "desde no puede ser null");
        Objects.requireNonNull(hasta, "hasta no puede ser null");
        if (hasta.isBefore(desde)) {
            throw new IllegalArgumentException(
                    "hasta (%s) no puede ser anterior a desde (%s)".formatted(hasta, desde));
        }
    }

    private static List<RendimientoCrudo> traducir(Document documento) {
        return documento.select(SELECTOR_FILA_PARTIDO).stream()
                .map(WhoScoredRepositoryImpl::traducir)
                .toList();
    }

    private static RendimientoCrudo traducir(Element filaPartido) {
        String partidoExternoId = filaPartido.attr("data-partido-id");
        LocalDate fechaPartido = LocalDate.parse(filaPartido.attr("data-fecha"));
        Map<String, Object> metricas = new LinkedHashMap<>();
        for (Element celdaMetrica : filaPartido.select(SELECTOR_METRICA)) {
            metricas.put(
                    celdaMetrica.attr("data-metrica"),
                    parsearValor(celdaMetrica.attr("data-valor")));
        }
        return new RendimientoCrudo(partidoExternoId, fechaPartido, metricas);
    }

    private static Object parsearValor(String valor) {
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException noEsEntero) {
            try {
                return Double.parseDouble(valor);
            } catch (NumberFormatException noEsDecimal) {
                return valor;
            }
        }
    }

    /** Extremo de infraestructura inyectable para no depender de red real en los tests. */
    @FunctionalInterface
    interface DocumentoFetcher {
        Document obtener(String url) throws IOException;
    }
}
