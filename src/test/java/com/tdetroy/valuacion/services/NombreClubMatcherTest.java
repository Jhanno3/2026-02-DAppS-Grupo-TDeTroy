package com.tdetroy.valuacion.services;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Cubre {@link NombreClubMatcher}, usado por {@link RendimientoServiceImpl} para resolver filas de
 * WhoScored contra el catálogo.
 */
class NombreClubMatcherTest {

    @Test
    void mismoNombreYClubExactos_coincide() {
        assertThat(
                        NombreClubMatcher.coincide(
                                "Kylian Mbappé",
                                "Real Madrid CF",
                                "Kylian Mbappé",
                                "Real Madrid CF"))
                .isTrue();
    }

    @Test
    void difierenEnAcentosYMayusculas_igualCoincide() {
        assertThat(
                        NombreClubMatcher.coincide(
                                "KYLIAN MBAPPE",
                                "real madrid cf",
                                "Kylian Mbappé",
                                "Real Madrid CF"))
                .isTrue();
    }

    @Test
    void difierenEnEspaciosExtra_igualCoincide() {
        assertThat(
                        NombreClubMatcher.coincide(
                                "Kylian   Mbappé",
                                "Real Madrid CF",
                                "Kylian Mbappé",
                                "Real Madrid CF"))
                .isTrue();
    }

    @Test
    void pequenaDiferenciaDeTipeoEnElNombre_igualCoincide() {
        // "Mbape" vs "Mbappé" normalizado ("mbape" vs "mbappe"): distancia de Levenshtein 1.
        assertThat(
                        NombreClubMatcher.coincide(
                                "Kylian Mbape",
                                "Real Madrid CF",
                                "Kylian Mbappé",
                                "Real Madrid CF"))
                .isTrue();
    }

    @Test
    void nombreMuyDistinto_noCoincide() {
        assertThat(
                        NombreClubMatcher.coincide(
                                "Jude Bellingham",
                                "Real Madrid CF",
                                "Kylian Mbappé",
                                "Real Madrid CF"))
                .isFalse();
    }

    @Test
    void mismoNombreClubDistinto_noCoincide() {
        assertThat(
                        NombreClubMatcher.coincide(
                                "Kylian Mbappé",
                                "Paris Saint-Germain",
                                "Kylian Mbappé",
                                "Real Madrid CF"))
                .isFalse();
    }

    // ---- club: nombre corto (WhoScored) vs nombre social completo (football-data.org) ----

    @Test
    void clubNombreCortoEsSubconjuntoDelNombreCompleto_coincide() {
        assertThat(
                        NombreClubMatcher.coincide(
                                "Robert Lewandowski",
                                "Barcelona",
                                "Robert Lewandowski",
                                "FC Barcelona"))
                .isTrue();
    }

    @Test
    void clubConSiglaYPreposicionDeMedio_coincide() {
        // "Club Atlético de Madrid" (football-data) vs "Atletico Madrid" (WhoScored, sin acento).
        assertThat(
                        NombreClubMatcher.coincide(
                                "Jugador", "Atletico Madrid", "Jugador", "Club Atlético de Madrid"))
                .isTrue();
    }

    @Test
    void clubConSiglaCortaDeDosLetras_coincide() {
        assertThat(NombreClubMatcher.coincide("Jugador", "Osasuna", "Jugador", "CA Osasuna"))
                .isTrue();
    }

    @Test
    void clubConNombreCompletoLargoYArticulo_coincide() {
        // "RC Deportivo La Coruña" (football-data) vs "Deportivo" (WhoScored).
        assertThat(
                        NombreClubMatcher.coincide(
                                "Jugador", "Deportivo", "Jugador", "RC Deportivo La Coruña"))
                .isTrue();
    }

    @Test
    void clubesDistintosQueComparténUnaPalabra_noCoincide() {
        // Comparten "Real" pero no son el mismo club -- no alcanza con intersección parcial.
        assertThat(NombreClubMatcher.coincide("Jugador", "Real Madrid", "Jugador", "Real Sociedad"))
                .isFalse();
    }

    // ---- nombre: orden de nombre/apellido invertido entre fuentes ----

    @Test
    void nombreYApellidoInvertidos_coincide() {
        // Caso real: WhoScored "Djené Dakonam" vs football-data.org "Dakonam Djené" (Getafe).
        assertThat(
                        NombreClubMatcher.coincide(
                                "Djené Dakonam", "Getafe CF", "Dakonam Djené", "Getafe CF"))
                .isTrue();
    }

    @Test
    void mismasPalabrasInvertidasPeroDistintaCapitalizacionYAcentos_coincide() {
        assertThat(
                        NombreClubMatcher.coincide(
                                "DJENE DAKONAM", "Getafe CF", "dakonam djené", "Getafe CF"))
                .isTrue();
    }

    @Test
    void ordenInvertidoConUnaPalabraDistinta_noCoincide() {
        // No alcanza con compartir una palabra en cualquier orden -- la otra palabra debe
        // coincidir.
        assertThat(
                        NombreClubMatcher.coincide(
                                "Djené Dakonam", "Getafe CF", "Dakonam Diallo", "Getafe CF"))
                .isFalse();
    }
}
