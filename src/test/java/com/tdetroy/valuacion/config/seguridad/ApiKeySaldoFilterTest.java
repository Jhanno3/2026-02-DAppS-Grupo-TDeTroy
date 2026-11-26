package com.tdetroy.valuacion.config.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.tdetroy.valuacion.config.errores.ProblemDetailResponseWriter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiKeySaldoFilterTest {

    @Test
    void doFilter_conApiKeyValida_continuaCadena() throws Exception {
        ProblemDetailResponseWriter writer = mock(ProblemDetailResponseWriter.class);
        ApiKeySaldoFilter filtro = new ApiKeySaldoFilter("mi-key", writer);
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/v1/usuarios/me/saldo");
        request.addHeader("X-API-Key", "mi-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filtro.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(writer);
    }

    @Test
    void doFilter_sinApiKey_responde401() throws Exception {
        ProblemDetailResponseWriter writer = mock(ProblemDetailResponseWriter.class);
        ApiKeySaldoFilter filtro = new ApiKeySaldoFilter("mi-key", writer);
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/v1/usuarios/me/saldo");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filtro.doFilter(request, response, chain);

        verify(writer)
                .escribir(eq(response), eq(HttpStatus.UNAUTHORIZED), eq("API Key inválida"), any());
        verifyNoInteractions(chain);
    }

    @Test
    void doFilter_enOtraRuta_noSeAplica() throws Exception {
        ProblemDetailResponseWriter writer = mock(ProblemDetailResponseWriter.class);
        ApiKeySaldoFilter filtro = new ApiKeySaldoFilter("mi-key", writer);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/jugadores");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filtro.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        verifyNoInteractions(writer);
    }
}
