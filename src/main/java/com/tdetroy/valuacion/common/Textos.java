package com.tdetroy.valuacion.common;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Normalización de texto compartida entre capas (constitution.md §2): minúsculas, sin acentos,
 * espacios colapsados. Usada tanto para matching de identidad (ver {@code
 * services.NombreClubMatcher}) como para resolver claves de configuración por nombre libre (ej.
 * liga → URL de WhoScored, ver {@code repositories.WhoScoredLigaRepositoryImpl}).
 */
public final class Textos {

    private Textos() {}

    public static String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        String sinAcentos =
                Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }
}
