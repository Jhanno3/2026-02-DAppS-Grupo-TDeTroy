package com.tdetroy.valuacion.config.trazabilidad;

import static org.assertj.core.api.Assertions.assertThat;

import com.tdetroy.valuacion.common.CorrelationId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest
@AutoConfigureMockMvc
class TrazabilidadIT {

    private MockMvcTester mvc;

    @Autowired
    void inicializarMvcTester(MockMvc mockMvc) {
        this.mvc = MockMvcTester.create(mockMvc);
    }

    @Test
    void unaRequestRechazada_tambienDevuelveElId() {
        assertThat(
                        mvc.get()
                                .uri("/api/v1/portfolios/me")
                                .header(CorrelationId.HEADER, "prueba-401")
                                .exchange())
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .hasHeader(CorrelationId.HEADER, "prueba-401");
    }

    @Test
    void cors_dejaQueElFrontLeaElId() {
        assertThat(
                        mvc.get()
                                .uri("/actuator/info")
                                .header("Origin", "http://localhost:5173")
                                .exchange())
                .hasHeader("Access-Control-Expose-Headers", CorrelationId.HEADER);
    }
}
