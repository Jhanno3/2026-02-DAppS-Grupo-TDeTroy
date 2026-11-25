package com.tdetroy.valuacion.config.auditoria;

import static org.assertj.core.api.Assertions.assertThat;

import com.tdetroy.valuacion.dto.usuario.LoginRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

class EnmascaradorParametrosTest {

    private final EnmascaradorParametros enmascarador =
            new EnmascaradorParametros(JsonMapper.builder().build());

    @ParameterizedTest
    @ValueSource(
            strings = {
                "password",
                "nuevaPassword",
                "contraseña",
                "apiKey",
                "X-API-Key",
                "api_key",
                "token",
                "Authorization",
                "numeroTarjeta",
                "cardNumber",
                "cvv",
                "email"
            })
    void unNombreSensible_seOculta(String nombre) {
        assertThat(enmascarador.enmascarar(nombre, "valor")).isEqualTo("***");
    }

    @Test
    void losCamposDelBody_seOcultan() {
        assertThat(enmascarador.enmascarar("request", new LoginRequest("a@b.com", "secreta")))
                .isEqualTo(Map.of("email", "***", "password", "***"));
    }

    @Test
    void losCamposAnidados_seOcultan() {
        Object valor = Map.of("pagos", List.of(Map.of("cvv", 123, "monto", 10)));

        assertThat(enmascarador.enmascarar("request", valor))
                .isEqualTo(Map.of("pagos", List.of(Map.of("cvv", "***", "monto", 10))));
    }

    @Test
    void unNumeroDeTarjeta_seOcultaConCualquierNombre() {
        assertThat(enmascarador.enmascarar("numero", "4111 1111 1111 1111")).isEqualTo("***");
        assertThat(enmascarador.enmascarar("numero", 4111111111111111L)).isEqualTo("***");
    }

    @Test
    void losDatosDelNegocio_seConservan() {
        UUID id = UUID.randomUUID();

        assertThat(enmascarador.enmascarar("id", id)).isEqualTo(id.toString());
        assertThat(enmascarador.enmascarar("cantidadTokens", 5)).isEqualTo(5);
        assertThat(enmascarador.enmascarar("semana", null)).isEqualTo("null");
    }

    @Test
    void unTextoLargo_seRecorta() {
        assertThat(enmascarador.enmascarar("busqueda", "a".repeat(600)))
                .isEqualTo("a".repeat(500) + "...");
    }
}
