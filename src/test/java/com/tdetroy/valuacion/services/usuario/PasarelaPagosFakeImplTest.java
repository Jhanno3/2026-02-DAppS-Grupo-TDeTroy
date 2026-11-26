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
    void procesarPago_numeroInvalido_lanzaPagoRechazado() {
        var tarjeta = new PasarelaPagos.DatosTarjeta("1234", "Juan Perez", "12/30", "123");

        assertThatThrownBy(() -> pasarela.procesarPago(new BigDecimal("100.00"), tarjeta))
                .isInstanceOf(PagoRechazadoException.class);
    }

    @Test
    void procesarPago_montoInvalido_lanzaPagoRechazado() {
        var tarjeta =
                new PasarelaPagos.DatosTarjeta("4532123456789010", "Juan Perez", "12/30", "123");

        assertThatThrownBy(() -> pasarela.procesarPago(BigDecimal.ZERO, tarjeta))
                .isInstanceOf(PagoRechazadoException.class);
    }
}
