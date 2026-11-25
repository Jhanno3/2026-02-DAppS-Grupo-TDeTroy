package com.tdetroy.valuacion.repositories.rendimiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.tdetroy.valuacion.common.CorrelationId;
import com.tdetroy.valuacion.config.WhoScoredLigaProperties;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class WhoScoredLigaCorrelationIdTest {

    @Test
    void reenviaElIdAlScraper() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo(Matchers.startsWith("http://scraper.test/fetch-liga")))
                .andExpect(header(CorrelationId.HEADER, "ingesta-1"))
                .andRespond(withSuccess("{\"pages\":[]}", MediaType.APPLICATION_JSON));
        WhoScoredLigaProperties propiedades = new WhoScoredLigaProperties();
        propiedades.setLigaUrls(Map.of("la-liga", "https://whoscored.test/la-liga"));
        WhoScoredLigaRepositoryImpl repository =
                new WhoScoredLigaRepositoryImpl(
                        builder,
                        propiedades,
                        "http://scraper.test/fetch-liga",
                        "https://imagenes.test");

        try (MDC.MDCCloseable ignorado = MDC.putCloseable(CorrelationId.CLAVE_MDC, "ingesta-1")) {
            assertThat(repository.obtenerAcumuladoLiga("La Liga")).isEmpty();
        }

        servidor.verify();
    }
}
