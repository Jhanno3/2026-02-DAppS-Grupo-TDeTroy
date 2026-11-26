package com.tdetroy.valuacion.services.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tdetroy.valuacion.common.exceptions.PagoRechazadoException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PasarelaPagosFakeImplTest {

    private final PasarelaPagosFakeImpl pasarela = new PasarelaPagosFakeImpl();

    @Test
    void procesarPago_datosValidos_retornaCodigoOperacionYUltimosDigitos() {
        var tarjeta =
                new PasarelaPagos.DatosTarjeta("4532123456789010", "Juan Perez", "12/30", "123");

        PasarelaPagos.ResultadoPago resultado =
                pasarela.procesarPago(new BigDecimal("250.00"), tarjeta);

        assertThat(resultado.codigoOperacion()).startsWith("PAY-");
        assertThat(resultado.ultimosCuatroDigitos()).isEqualTo("9010");
    }

    @Test
    void procesarPago_tarjetaVencida_lanzaPagoRechazado() {
        var tarjeta =
                new PasarelaPagos.DatosTarjeta("4532123456789010", "Juan Perez", "01/20", "123");

        assertThatThrownBy(() -> pasarela.procesarPago(new BigDecimal("100.00"), tarjeta))
                .isInstanceOf(PagoRechazadoException.class)
                .hasMessageContaining("vencida");
    }

    @Test
    void procesarPago_numeroInvalidoONull_lanzaPagoRechazado() {
        var tarjetaCorta = new PasarelaPagos.DatosTarjeta("1234", "Juan Perez", "12/30", "123");
        var tarjetaNull = new PasarelaPagos.DatosTarjeta(null, "Juan Perez", "12/30", "123");

        assertThatThrownBy(() -> pasarela.procesarPago(new BigDecimal("100.00"), tarjetaCorta))
                .isInstanceOf(PagoRechazadoException.class);
        assertThatThrownBy(() -> pasarela.procesarPago(new BigDecimal("100.00"), tarjetaNull))
                .isInstanceOf(PagoRechazadoException.class);
        assertThatThrownBy(() -> pasarela.procesarPago(new BigDecimal("100.00"), null))
                .isInstanceOf(PagoRechazadoException.class);
    }

    @Test
    void procesarPago_titularCvvOVencimientoInvalidos_lanzaPagoRechazado() {
        var sinTitular = new PasarelaPagos.DatosTarjeta("4532123456789010", "  ", "12/30", "123");
        var cvvInvalido =
                new PasarelaPagos.DatosTarjeta("4532123456789010", "Juan Perez", "12/30", "1");
        var vencimientoInvalido =
                new PasarelaPagos.DatosTarjeta("4532123456789010", "Juan Perez", "99/99", "123");
        var vencimientoNull =
                new PasarelaPagos.DatosTarjeta("4532123456789010", "Juan Perez", null, "123");

        assertThatThrownBy(() -> pasarela.procesarPago(new BigDecimal("100.00"), sinTitular))
                .isInstanceOf(PagoRechazadoException.class);
        assertThatThrownBy(() -> pasarela.procesarPago(new BigDecimal("100.00"), cvvInvalido))
                .isInstanceOf(PagoRechazadoException.class);
        assertThatThrownBy(
                        () -> pasarela.procesarPago(new BigDecimal("100.00"), vencimientoInvalido))
                .isInstanceOf(PagoRechazadoException.class);
        assertThatThrownBy(() -> pasarela.procesarPago(new BigDecimal("100.00"), vencimientoNull))
                .isInstanceOf(PagoRechazadoException.class);
    }

    @Test
    void procesarPago_montoInvalidoONull_lanzaPagoRechazado() {
        var tarjeta =
                new PasarelaPagos.DatosTarjeta("4532123456789010", "Juan Perez", "12/30", "123");

        assertThatThrownBy(() -> pasarela.procesarPago(BigDecimal.ZERO, tarjeta))
                .isInstanceOf(PagoRechazadoException.class);
        assertThatThrownBy(() -> pasarela.procesarPago(null, tarjeta))
                .isInstanceOf(PagoRechazadoException.class);
    }
}
