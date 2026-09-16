package com.tdetroy.valuacion.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * {@link ProblemDetailResponseWriter} es la única pieza de lógica propia detrás de los 401/403
 * que emite {@link SecurityConfig} — se prueba en aislamiento acá para no depender de levantar
 * el filtro de seguridad completo con un usuario autenticado (eso llega recién con JWT en T1.4).
 */
class ProblemDetailResponseWriterTest {

    private final ProblemDetailResponseWriter writer = new ProblemDetailResponseWriter(new ObjectMapper());

    @Test
    void escribeStatusYContentTypeProblemJson() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        writer.escribir(response, HttpStatus.UNAUTHORIZED, "No autenticado", "Se requiere un token válido.");

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    }

    @Test
    void elCuerpoContieneTituloDetalleYStatus() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        writer.escribir(response, HttpStatus.FORBIDDEN, "Acceso denegado", "No tenés permisos.");
        String cuerpo = response.getContentAsString();

        assertThat(cuerpo)
                .contains("\"title\":\"Acceso denegado\"")
                .contains("\"detail\":\"No tenés permisos.\"")
                .contains("\"status\":403");
    }
}
