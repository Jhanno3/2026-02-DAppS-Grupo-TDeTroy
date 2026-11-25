package com.tdetroy.valuacion.config.trazabilidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tdetroy.valuacion.common.CorrelationId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    private final AtomicReference<String> idEnLosLogs = new AtomicReference<>();

    @Test
    void sinHeader_generaUnIdDistintoPorRequest() throws Exception {
        String primero = filtrar(null);
        String segundo = filtrar(null);

        assertThat(primero).isNotBlank().isNotEqualTo(segundo);
    }

    @Test
    void conIdValido_loRespetaYLoLimpiaAlTerminar() throws Exception {
        assertThat(filtrar("pedido-123")).isEqualTo("pedido-123");
        assertThat(idEnLosLogs).hasValue("pedido-123");
        assertThat(CorrelationId.actual()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "con espacio", "a\nERROR falso", "<script>"})
    void conIdInvalido_loReemplaza(String recibido) throws Exception {
        assertThat(filtrar(recibido)).isNotEqualTo(recibido).hasSize(36);
    }

    @Test
    void conIdDemasiadoLargo_loReemplaza() throws Exception {
        assertThat(filtrar("a".repeat(65))).hasSize(36);
    }

    @Test
    void siLaRequestFalla_igualLimpiaElId() {
        FilterChain cadena =
                (request, response) -> {
                    throw new ServletException("falla");
                };

        assertThatThrownBy(
                        () ->
                                new CorrelationIdFilter()
                                        .doFilter(
                                                new MockHttpServletRequest(),
                                                new MockHttpServletResponse(),
                                                cadena))
                .isInstanceOf(ServletException.class);
        assertThat(CorrelationId.actual()).isEmpty();
    }

    private String filtrar(String recibido) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (recibido != null) {
            request.addHeader(CorrelationId.HEADER, recibido);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain cadena = (req, res) -> idEnLosLogs.set(CorrelationId.actual().orElse(null));

        new CorrelationIdFilter().doFilter(request, response, cadena);

        assertThat(response.getHeader(CorrelationId.HEADER)).isEqualTo(idEnLosLogs.get());
        return idEnLosLogs.get();
    }
}
