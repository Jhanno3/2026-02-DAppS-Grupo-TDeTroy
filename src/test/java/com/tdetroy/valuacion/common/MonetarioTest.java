package com.tdetroy.valuacion.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.junit.jupiter.api.Test;

class MonetarioTest {

    @Test
    void escalaEsDos() {
        assertThat(Monetario.ESCALA).isEqualTo(2);
    }

    @Test
    void redondeoEsHalfUp() {
        assertThat(Monetario.REDONDEO).isEqualTo(RoundingMode.HALF_UP);
    }

    @Test
    void ceroYaEstaEscaladoAEscalaDos() {
        assertThat(Monetario.CERO).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(Monetario.CERO.scale()).isEqualTo(2);
    }

    @Test
    void escalarRedondeaHaciaArribaEnEmpateExacto() {
        // HALF_UP: el tercer decimal exactamente 5 redondea lejos de cero.
        assertThat(Monetario.escalar(new BigDecimal("10.005"))).isEqualByComparingTo("10.01");
        assertThat(Monetario.escalar(new BigDecimal("-10.005"))).isEqualByComparingTo("-10.01");
    }

    @Test
    void escalarRedondeaHaciaAbajoPorDebajoDelEmpate() {
        assertThat(Monetario.escalar(new BigDecimal("10.004"))).isEqualByComparingTo("10.00");
    }

    @Test
    void escalarCompletaDecimalesFaltantesSinRedondear() {
        assertThat(Monetario.escalar(new BigDecimal("10"))).isEqualByComparingTo("10.00");
        assertThat(Monetario.escalar(new BigDecimal("10")).scale()).isEqualTo(2);
    }

    @Test
    void escalarEsIdempotenteSobreUnValorYaEscalado() {
        BigDecimal yaEscalado = new BigDecimal("7.50");
        assertThat(Monetario.escalar(yaEscalado)).isEqualByComparingTo(yaEscalado);
    }

    @Test
    void escalarRechazaNull() {
        assertThatThrownBy(() -> Monetario.escalar(null))
                .isInstanceOf(NullPointerException.class);
    }
}
