package com.tdetroy.valuacion.common.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Verifica que la jerarquía base de excepciones de negocio cuelga toda de
 * {@link NegocioException} (contrato que consumirá el manejador global de errores de T0.4) y
 * que cada mensaje queda formado de forma consistente y sin perder el detalle necesario
 * para diagnóstico interno.
 */
class ExcepcionesNegocioTest {

    @Test
    void saldoInsuficienteEsNegocioExceptionYArmaElMensaje() {
        var ex = new SaldoInsuficienteException(new BigDecimal("100.00"), new BigDecimal("40.00"));

        assertThat(ex).isInstanceOf(NegocioException.class);
        assertThat(ex.getMessage())
                .contains("100.00")
                .contains("40.00");
    }

    @Test
    void tenenciaInsuficienteEsNegocioExceptionYArmaElMensaje() {
        var ex = new TenenciaInsuficienteException(10, 3);

        assertThat(ex).isInstanceOf(NegocioException.class);
        assertThat(ex.getMessage())
                .contains("10")
                .contains("3");
    }

    @Test
    void emisionMaximaSuperadaEsNegocioExceptionYArmaElMensaje() {
        var ex = new EmisionMaximaSuperadaException(95, 10, 100);

        assertThat(ex).isInstanceOf(NegocioException.class);
        assertThat(ex.getMessage())
                .contains("95")
                .contains("10")
                .contains("100");
    }
}
