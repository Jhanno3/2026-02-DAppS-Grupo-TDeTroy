package com.tdetroy.valuacion.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Mapea liga → URL de la página pública de estadísticas de liga de WhoScored (plan.md §8.1), config
 * aparte de la lógica de scraping — agregar una liga nueva es sólo una entrada más de
 * configuración, sin tocar código.
 *
 * <p>Clave esperada en {@code application.properties} (prefijo {@code app.whoscored.liga-urls}): el
 * nombre de la liga normalizado (sin acentos, minúsculas, espacios por guiones — ver {@code
 * com.tdetroy.valuacion.common.Textos#normalizar}), ej. {@code la-liga} para {@code Jugador.liga =
 * "La Liga"}. Hoy sólo se completa {@code la-liga}.
 */
@Component
@ConfigurationProperties(prefix = "app.whoscored")
public class WhoScoredLigaProperties {

    private Map<String, String> ligaUrls = new HashMap<>();

    public Map<String, String> getLigaUrls() {
        return ligaUrls;
    }

    public void setLigaUrls(Map<String, String> ligaUrls) {
        this.ligaUrls = ligaUrls;
    }
}
