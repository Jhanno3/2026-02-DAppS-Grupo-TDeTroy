package com.tdetroy.valuacion.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Test de {@link FootballDataRepositoryImpl} contra fixtures propios (constitution.md §4:
 * "prohibido que un test unitario dependa de red real... los adapters de WhoScored y
 * Football-Data.org no deben depender de la disponibilidad real de esos sitios/APIs"), nunca contra
 * la API real. {@link MockRestServiceServer} intercepta el {@link RestClient} interno de la
 * implementación antes de que llegue a la red.
 */
class FootballDataRepositoryImplTest {

    private static final String BASE_URL = "https://api.football-data.org";
    private static final String API_KEY = "clave-de-test";

    private MockRestServiceServer mockServer;
    private FootballDataRepositoryImpl repository;

    private void construir() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        repository = new FootballDataRepositoryImpl(builder, BASE_URL, API_KEY);
    }

    @Test
    void partidosFinalizados_seTraducenAPartidoCrudo() {
        construir();
        mockServer
                .expect(
                        requestTo(
                                BASE_URL
                                        + "/v4/matches?dateFrom=2026-02-09&dateTo=2026-02-15&status=FINISHED"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Auth-Token", API_KEY))
                .andRespond(
                        withSuccess(
                                new ClassPathResource(
                                        "fixtures/football-data/partidos-finalizados.json"),
                                MediaType.APPLICATION_JSON));

        List<PartidoCrudo> partidos =
                repository.obtenerPartidosDisputados(
                        LocalDate.of(2026, 2, 9), LocalDate.of(2026, 2, 15));

        assertThat(partidos).hasSize(2);

        PartidoCrudo primero = partidos.get(0);
        assertThat(primero.partidoExternoId()).isEqualTo("12345");
        assertThat(primero.fecha()).isEqualTo(LocalDate.of(2026, 2, 15));
        assertThat(primero.estado()).isEqualTo("FINISHED");
        assertThat(primero.equipoLocal()).isEqualTo("Club A");
        assertThat(primero.equipoVisitante()).isEqualTo("Club B");
        assertThat(primero.golesLocal()).isEqualTo(2);
        assertThat(primero.golesVisitante()).isEqualTo(1);

        PartidoCrudo segundo = partidos.get(1);
        assertThat(segundo.partidoExternoId()).isEqualTo("67890");
        assertThat(segundo.golesLocal()).isEqualTo(0);
        assertThat(segundo.golesVisitante()).isEqualTo(0);

        mockServer.verify();
    }

    @Test
    void fallaHttpDeLaApi_seAislaYDevuelveListaVacia() {
        construir();
        mockServer
                .expect(
                        requestTo(
                                BASE_URL
                                        + "/v4/matches?dateFrom=2026-02-09&dateTo=2026-02-15&status=FINISHED"))
                .andRespond(withServerError());

        List<PartidoCrudo> partidos =
                repository.obtenerPartidosDisputados(
                        LocalDate.of(2026, 2, 9), LocalDate.of(2026, 2, 15));

        assertThat(partidos).isEmpty();
    }

    @Test
    void respuestaSinCampoMatches_devuelveListaVacia() {
        construir();
        mockServer
                .expect(
                        requestTo(
                                BASE_URL
                                        + "/v4/matches?dateFrom=2026-02-09&dateTo=2026-02-15&status=FINISHED"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        List<PartidoCrudo> partidos =
                repository.obtenerPartidosDisputados(
                        LocalDate.of(2026, 2, 9), LocalDate.of(2026, 2, 15));

        assertThat(partidos).isEmpty();
    }

    @Test
    void desdeNulo_lanzaExcepcion() {
        construir();
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                repository.obtenerPartidosDisputados(
                                        null, LocalDate.of(2026, 2, 15)));
    }

    @Test
    void hastaNulo_lanzaExcepcion() {
        construir();
        assertThatNullPointerException()
                .isThrownBy(
                        () -> repository.obtenerPartidosDisputados(LocalDate.of(2026, 2, 9), null));
    }

    @Test
    void hastaAnteriorADesde_lanzaExcepcion() {
        construir();
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                repository.obtenerPartidosDisputados(
                                        LocalDate.of(2026, 2, 15), LocalDate.of(2026, 2, 9)));
    }
}
