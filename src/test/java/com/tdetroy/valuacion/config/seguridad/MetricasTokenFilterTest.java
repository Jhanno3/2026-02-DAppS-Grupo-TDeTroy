package com.tdetroy.valuacion.config.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class MetricasTokenFilterTest {

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void conElTokenCorrecto_daSoloLaAutoridadDeMetricas() throws Exception {
        Authentication autenticacion =
                filtrar("secreto", MetricasTokenFilter.RUTA, "Bearer secreto");

        assertThat(autenticacion).isNotNull();
        assertThat(autenticacion.getAuthorities())
                .extracting(Object::toString)
                .containsExactly(MetricasTokenFilter.AUTORIDAD);
    }

    @Test
    void conOtroTokenOSinHeader_noAutentica() throws Exception {
        assertThat(filtrar("secreto", MetricasTokenFilter.RUTA, "Bearer otro")).isNull();
        assertThat(filtrar("secreto", MetricasTokenFilter.RUTA, null)).isNull();
    }

    @Test
    void enOtraRuta_noAutentica() throws Exception {
        assertThat(filtrar("secreto", "/actuator/metrics", "Bearer secreto")).isNull();
    }

    @Test
    void sinTokenConfigurado_noAutenticaANadie() throws Exception {
        assertThat(filtrar("", MetricasTokenFilter.RUTA, "Bearer ")).isNull();
        assertThat(filtrar(null, MetricasTokenFilter.RUTA, "Bearer ")).isNull();
    }

    private static Authentication filtrar(String token, String ruta, String header)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", ruta);
        request.setServletPath(ruta);
        if (header != null) {
            request.addHeader("Authorization", header);
        }
        new MetricasTokenFilter(token)
                .doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
