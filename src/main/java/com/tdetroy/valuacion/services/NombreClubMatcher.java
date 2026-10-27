package com.tdetroy.valuacion.services;

import com.tdetroy.valuacion.common.Textos;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resuelve si una fila de rendimiento de liga de WhoScored (nombre + club, en texto libre tal como
 * lo expone esa fuente) corresponde al mismo jugador que uno del catálogo (plan.md §8.1) —
 * normaliza acentos/mayúsculas/espacios ({@link Textos#normalizar}) y tolera: (a) un error de tipeo
 * o variación menor vía distancia de Levenshtein, y (b) nombre y apellido en orden invertido entre
 * fuentes (ej. WhoScored "Djené Dakonam" vs football-data.org "Dakonam Djené") comparando el
 * conjunto de palabras del nombre sin importar el orden — comprobado como caso real, no hipotético.
 *
 * <p>Club exige coincidencia por **conjunto de tokens**, no por igualdad de texto: WhoScored
 * publica el nombre corto y usual de cada club ("Barcelona", "Atletico Madrid", "Osasuna"),
 * mientras que la fuente de catálogo (football-data.org) usa el nombre social completo ("FC
 * Barcelona", "Club Atlético de Madrid", "CA Osasuna") — comprobado contra los 20 clubes reales de
 * La Liga: en todos los casos el nombre corto es, token por token (ignorando siglas de forma
 * societaria), un subconjunto del nombre completo. Dos clubes coinciden si, tras normalizar y
 * descartar siglas/stopwords, el conjunto de palabras de uno es subconjunto del otro (en cualquier
 * sentido) — exigir igualdad de texto completa hacía fallar la resolución de la inmensa mayoría de
 * las filas (sólo matcheaban los pocos clubes cuyo nombre corto coincide letra por letra).
 *
 * <p>Usado exclusivamente por {@code RendimientoServiceImpl} — package-private, no es un contrato
 * público de {@code services/}.
 */
final class NombreClubMatcher {

    /**
     * Máxima distancia de edición entre nombres normalizados para considerarlos la misma persona.
     * Asunción técnica documentada (plan.md §8.1): tolera un error de tipeo o una variación menor
     * (ej. con/sin segundo nombre abreviado), sin volverse tan laxa que confunda jugadores
     * distintos del mismo club.
     */
    private static final int DISTANCIA_MAXIMA_NOMBRE = 2;

    /**
     * Siglas de forma societaria / palabras de relleno que varían entre fuentes sin cambiar la
     * identidad del club (ej. "FC Barcelona" vs "Barcelona", "RC Deportivo La Coruña" vs
     * "Deportivo"). Se descartan antes de comparar conjuntos de tokens.
     */
    private static final Set<String> STOPWORDS_CLUB =
            Set.of("cf", "fc", "rc", "rcd", "ud", "cd", "sd", "ca", "club", "de", "la");

    private NombreClubMatcher() {}

    static boolean coincide(String nombreA, String clubA, String nombreB, String clubB) {
        if (!clubesCoinciden(clubA, clubB)) {
            return false;
        }
        return nombresCoinciden(nombreA, nombreB);
    }

    private static boolean nombresCoinciden(String nombreA, String nombreB) {
        String normalizadoA = Textos.normalizar(nombreA);
        String normalizadoB = Textos.normalizar(nombreB);
        if (normalizadoA.equals(normalizadoB)) {
            return true;
        }
        if (palabrasOrdenadas(normalizadoA).equals(palabrasOrdenadas(normalizadoB))) {
            return true;
        }
        return distanciaLevenshtein(normalizadoA, normalizadoB) <= DISTANCIA_MAXIMA_NOMBRE;
    }

    /**
     * Palabras del nombre, ordenadas alfabéticamente -- vuelve irrelevante el orden
     * nombre/apellido.
     */
    private static String palabrasOrdenadas(String nombreNormalizado) {
        return Arrays.stream(nombreNormalizado.split("\\s+"))
                .filter(palabra -> !palabra.isBlank())
                .sorted()
                .collect(Collectors.joining(" "));
    }

    private static boolean clubesCoinciden(String clubA, String clubB) {
        Set<String> tokensA = tokensSignificativos(clubA);
        Set<String> tokensB = tokensSignificativos(clubB);
        if (tokensA.isEmpty() || tokensB.isEmpty()) {
            return false;
        }
        return tokensB.containsAll(tokensA) || tokensA.containsAll(tokensB);
    }

    private static Set<String> tokensSignificativos(String club) {
        return Arrays.stream(Textos.normalizar(club).split("\\s+"))
                .filter(token -> !token.isBlank() && !STOPWORDS_CLUB.contains(token))
                .collect(Collectors.toSet());
    }

    private static int distanciaLevenshtein(String a, String b) {
        int[][] distancias = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            distancias[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            distancias[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int costoSustitucion = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                distancias[i][j] =
                        Math.min(
                                Math.min(distancias[i - 1][j] + 1, distancias[i][j - 1] + 1),
                                distancias[i - 1][j - 1] + costoSustitucion);
            }
        }
        return distancias[a.length()][b.length()];
    }
}
