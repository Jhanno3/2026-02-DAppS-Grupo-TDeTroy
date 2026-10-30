package com.tdetroy.valuacion.repositories;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tdetroy.valuacion.common.Textos;
import com.tdetroy.valuacion.config.WhoScoredLigaProperties;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * {@inheritDoc}
 *
 * <p>Scrapea la página pública de estadísticas de liga de WhoScored (constitution §1: fuente no
 * oficial) a través del sidecar de Playwright ({@code scraper-service}, endpoint {@code
 * /fetch-liga}, config {@code app.whoscored.fetcher-liga-url}): ese sidecar es quien clickea el
 * filtro "All players" y pagina con "next" dentro de un browser real (necesario para esquivar
 * Cloudflare y ejecutar el JS de la página), y devuelve el HTML ya renderizado de cada página
 * recorrida. Esta clase sólo traduce ese HTML a {@link JugadorRendimientoLigaCrudo}, nunca hace la
 * navegación ella misma.
 *
 * <p><b>Estructura de HTML real de WhoScored (verificada contra la página pública de estadísticas
 * de liga, plan.md §8.1):</b> las filas de jugador viven en {@code
 * tbody#player-table-statistics-body > tr}; dentro de cada fila, {@code td.grid-ghost-cell} trae el
 * nombre ({@code a.player-link}, texto limpio) y el club ({@code a.player-meta-data
 * span.team-name}, con una coma y espacio al final que se recorta). Las métricas curadas que esta
 * fuente provee como acumulado de temporada viven en celdas con clase propia que coincide casi
 * literal con nuestras claves internas. El link del nombre ({@code href="/players/{id}/show/..."})
 * trae el id de WhoScored del jugador, con el que se arma la URL de su foto en el CDN de imágenes
 * ({@code app.whoscored.imagenes-url} + {@code players/{id}.jpg}) sin otra request — {@code
 * minsPlayed}, {@code yellowCard}, {@code redCard}, {@code manOfTheMatch} son iguales; {@code
 * goal}→{@code goals} y {@code assistTotal}→{@code assists} difieren sólo en el nombre. {@code
 * apps} no tiene clase propia (es la 3ª celda de la fila, inmediatamente después de la celda
 * "ghost") y puede venir como {@code "N"} (titular siempre) o {@code "N(M)"} (N titular + M de
 * suplente, que se suman). Un valor en {@code "-"} significa 0 (WhoScored no imprime cero desnudo
 * en estas columnas). Esta fuente no es una API versionada: un cambio de maquetación real puede
 * romper estos selectores sin aviso — mismo riesgo ya documentado en plan.md §11, mitigado igual
 * que el resto de esta clase (aislamiento de errores, nunca tumba el resto de la ingesta).
 *
 * <p>Aísla sus propios errores igual que el resto de los repositories de fuentes externas
 * (constitution §1): liga sin URL configurada, falla de red/parseo o cambio de estructura nunca
 * lanzan, siempre devuelven lista vacía y quedan logueados acá.
 */
@Repository
public class WhoScoredLigaRepositoryImpl implements RendimientoLigaExternoRepository {

    private static final Logger log = LoggerFactory.getLogger(WhoScoredLigaRepositoryImpl.class);

    private static final String SELECTOR_FILA_JUGADOR = "tbody#player-table-statistics-body > tr";
    private static final String SELECTOR_CELDA_GHOST = "td.grid-ghost-cell";
    private static final Pattern ID_EN_LINK_JUGADOR =
            Pattern.compile("/players/(\\d+)/", Pattern.CASE_INSENSITIVE);

    private final Map<String, String> urlsPorLiga;
    private final PaginasFetcher paginasFetcher;
    private final String imagenesUrl;

    @Autowired
    public WhoScoredLigaRepositoryImpl(
            RestClient.Builder restClientBuilder,
            WhoScoredLigaProperties ligaProperties,
            @Value("${app.whoscored.fetcher-liga-url:}") String fetcherLigaUrl,
            @Value("${app.whoscored.imagenes-url}") String imagenesUrl) {
        this(
                ligaProperties.getLigaUrls(),
                construirFetcherSidecar(restClientBuilder.build(), fetcherLigaUrl),
                imagenesUrl);
    }

    WhoScoredLigaRepositoryImpl(
            Map<String, String> urlsPorLiga, PaginasFetcher paginasFetcher, String imagenesUrl) {
        this.urlsPorLiga = urlsPorLiga;
        this.paginasFetcher = paginasFetcher;
        this.imagenesUrl = imagenesUrl.endsWith("/") ? imagenesUrl : imagenesUrl + "/";
    }

    @Override
    public List<JugadorRendimientoLigaCrudo> obtenerAcumuladoLiga(String liga) {
        Objects.requireNonNull(liga, "liga no puede ser null");
        String claveLiga = Textos.normalizar(liga).replace(' ', '-');
        String url = urlsPorLiga.get(claveLiga);
        if (url == null || url.isBlank()) {
            log.warn(
                    "No hay URL de WhoScored configurada para la liga '{}' (clave esperada '{}')",
                    liga,
                    claveLiga);
            return List.of();
        }
        try {
            List<Document> paginas = paginasFetcher.obtenerPaginas(url);
            List<JugadorRendimientoLigaCrudo> resultado = new ArrayList<>();
            for (Document pagina : paginas) {
                resultado.addAll(traducir(pagina));
            }
            return resultado;
        } catch (IOException | RuntimeException ex) {
            log.error("Fallo al scrapear la liga '{}' de WhoScored: {}", liga, ex.getMessage(), ex);
            return List.of();
        }
    }

    private List<JugadorRendimientoLigaCrudo> traducir(Document pagina) {
        return pagina.select(SELECTOR_FILA_JUGADOR).stream().map(this::traducirFila).toList();
    }

    private JugadorRendimientoLigaCrudo traducirFila(Element filaJugador) {
        Element celdaGhost = filaJugador.selectFirst(SELECTOR_CELDA_GHOST);
        String nombre = celdaGhost == null ? "" : textoDe(celdaGhost, "a.player-link");
        String fotoUrl =
                celdaGhost == null ? null : fotoUrlDe(celdaGhost.selectFirst("a.player-link"));
        String club =
                quitarComaFinal(celdaGhost == null ? "" : textoDe(celdaGhost, "span.team-name"));

        List<Element> celdas = filaJugador.select("> td");

        Map<String, Object> metricas = new LinkedHashMap<>();
        metricas.put("apps", parsearApps(celdas.size() > 2 ? celdas.get(2).text() : ""));
        metricas.put("minsPlayed", parsearEntero(filaJugador, "td.minsPlayed"));
        metricas.put("goals", parsearEntero(filaJugador, "td.goal"));
        metricas.put("assists", parsearEntero(filaJugador, "td.assistTotal"));
        metricas.put("yellowCard", parsearEntero(filaJugador, "td.yellowCard"));
        metricas.put("redCard", parsearEntero(filaJugador, "td.redCard"));
        metricas.put("manOfTheMatch", parsearEntero(filaJugador, "td.manOfTheMatch"));

        return new JugadorRendimientoLigaCrudo(nombre, club, metricas, fotoUrl);
    }

    private String fotoUrlDe(Element linkJugador) {
        if (linkJugador == null) {
            return null;
        }
        Matcher id = ID_EN_LINK_JUGADOR.matcher(linkJugador.attr("href"));
        return id.find() ? imagenesUrl + "players/" + id.group(1) + ".jpg" : null;
    }

    private static String textoDe(Element padre, String selector) {
        Element elemento = padre.selectFirst(selector);
        return elemento == null ? "" : elemento.text().trim();
    }

    private static String quitarComaFinal(String texto) {
        return texto.replaceAll(",\\s*$", "").trim();
    }

    private static int parsearEntero(Element fila, String selector) {
        Element celda = fila.selectFirst(selector);
        return celda == null ? 0 : parsearApps(celda.text());
    }

    /**
     * Parsea un valor numérico de celda de WhoScored: {@code "-"} es 0 (WhoScored no imprime cero
     * desnudo en estas columnas), y un formato {@code "N(M)"} (titular + suplente, sólo visto en la
     * columna de apariciones) suma ambas partes.
     */
    private static int parsearApps(String texto) {
        String limpio = texto == null ? "" : texto.trim();
        if (limpio.isEmpty() || limpio.equals("-")) {
            return 0;
        }
        if (limpio.contains("(")) {
            int total = 0;
            for (String parte : limpio.replace(")", "").split("\\(")) {
                total += parsearEnteroSimple(parte);
            }
            return total;
        }
        return parsearEnteroSimple(limpio);
    }

    private static int parsearEnteroSimple(String valor) {
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException noEsEntero) {
            return 0;
        }
    }

    private static PaginasFetcher construirFetcherSidecar(
            RestClient restClient, String fetcherLigaUrl) {
        return url -> {
            if (fetcherLigaUrl == null || fetcherLigaUrl.isBlank()) {
                throw new IOException(
                        "app.whoscored.fetcher-liga-url no esta configurado; la ingesta por liga"
                                + " requiere el sidecar de Playwright");
            }
            RespuestaPaginas respuesta;
            try {
                respuesta =
                        restClient
                                .get()
                                .uri(fetcherLigaUrl + "?url={url}", url)
                                .retrieve()
                                .body(RespuestaPaginas.class);
            } catch (RestClientException ex) {
                throw new IOException(ex.getMessage(), ex);
            }
            if (respuesta == null || respuesta.pages() == null) {
                return List.of();
            }
            return respuesta.pages().stream().map(html -> Jsoup.parse(html, url)).toList();
        };
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RespuestaPaginas(List<String> pages) {}

    /** Extremo de infraestructura inyectable para no depender de red real en los tests. */
    @FunctionalInterface
    interface PaginasFetcher {
        List<Document> obtenerPaginas(String url) throws IOException;
    }
}
