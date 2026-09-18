package com.tdetroy.valuacion.config;

import static org.springframework.test.web.servlet.assertj.MockMvcTester.create;

import com.tdetroy.valuacion.common.exceptions.SaldoInsuficienteException;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Prueba {@link GlobalExceptionHandler} contra un controller mínimo propio del test (no hay ningún
 * {@code Controller} real todavía, T0.4 es transversal), verificando exactamente el contrato de
 * plan.md §3: 409 para una {@link com.tdetroy.valuacion.common.exceptions.NegocioException}, 500
 * genérico sin filtrar el mensaje interno para cualquier otro error no anticipado.
 */
class GlobalExceptionHandlerTest {

    private MockMvcTester mvc;

    @BeforeEach
    void setUp() {
        mvc =
                create(
                        MockMvcBuilders.standaloneSetup(new ControllerDePrueba())
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .build());
    }

    @Test
    void negocioExceptionResponde409ConProblemJsonYElMensajeDeNegocio() {
        mvc.get()
                .uri("/test/negocio")
                .assertThat()
                .hasStatus(409)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyText()
                .contains("Saldo insuficiente")
                .contains("Regla de negocio violada");
    }

    @Test
    void errorInesperadoResponde500SinFiltrarElMensajeInterno() {
        mvc.get()
                .uri("/test/inesperado")
                .assertThat()
                .hasStatus(500)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyText()
                .contains("Ocurrió un error inesperado")
                .doesNotContain("secreto-interno-de-la-excepcion");
    }

    @RestController
    static class ControllerDePrueba {

        @GetMapping("/test/negocio")
        void negocio() {
            throw new SaldoInsuficienteException(new BigDecimal("100.00"), new BigDecimal("10.00"));
        }

        @GetMapping("/test/inesperado")
        void inesperado() {
            throw new IllegalStateException("secreto-interno-de-la-excepcion");
        }
    }
}
