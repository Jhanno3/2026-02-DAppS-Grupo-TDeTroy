package com.tdetroy.valuacion.config.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class ApiKeyRecalculoFilterTest {

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_conApiKeyValidaEnRutaRecalculo_autenticaSinJwt() throws Exception {
        ApiKeyRecalculoFilter filtro = new ApiKeyRecalculoFilter("recalculo-dev-api-key");
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/v1/cotizaciones/recalculo");
        request.addHeader("X-API-Key", "recalculo-dev-api-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filtro.doFilter(request, response, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities())
                .extracting("authority")
                .containsExactly(ApiKeyRecalculoFilter.AUTORIDAD);
    }

    @Test
    void doFilter_conApiKeyInvalida_noAutentica() throws Exception {
        ApiKeyRecalculoFilter filtro = new ApiKeyRecalculoFilter("recalculo-dev-api-key");
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/v1/cotizaciones/recalculo");
        request.addHeader("X-API-Key", "clave-incorrecta");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(request, response, new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_enOtraRuta_noSeAplica() throws Exception {
        ApiKeyRecalculoFilter filtro = new ApiKeyRecalculoFilter("recalculo-dev-api-key");
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/v1/cotizaciones/ingesta-rendimiento");
        request.addHeader("X-API-Key", "recalculo-dev-api-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(request, response, new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
