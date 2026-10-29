package com.tdetroy.valuacion.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ConfiguracionCotizacionTest {

    @Test
    void configurar_conEstrategia_quedaConEsaEstrategiaYFecha() {
        ConfiguracionCotizacion configuracion =
                ConfiguracionCotizacion.configurar(EstrategiaCotizacion.RENDIMIENTO_ACTUAL);

        assertThat(configuracion.getEstrategiaDefault())
                .isEqualTo(EstrategiaCotizacion.RENDIMIENTO_ACTUAL);
        assertThat(configuracion.getFechaActualizacion()).isNotNull();
    }

    @Test
    void configurar_conEstrategiaNula_lanzaExcepcion() {
        assertThatNullPointerException().isThrownBy(() -> ConfiguracionCotizacion.configurar(null));
    }

    @Test
    void cambiarEstrategiaDefault_actualizaEstrategiaYFecha() {
        Instant antes = Instant.parse("2026-01-01T00:00:00Z");
        ConfiguracionCotizacion configuracion =
                ConfiguracionCotizacion.reconstruir(EstrategiaCotizacion.RENDIMIENTO_ACTUAL, antes);

        configuracion.cambiarEstrategiaDefault(EstrategiaCotizacion.RENDIMIENTO_TEMPORADA);

        assertThat(configuracion.getEstrategiaDefault())
                .isEqualTo(EstrategiaCotizacion.RENDIMIENTO_TEMPORADA);
        assertThat(configuracion.getFechaActualizacion()).isAfter(antes);
    }

    @Test
    void cambiarEstrategiaDefault_conNula_lanzaExcepcionYConservaLaAnterior() {
        ConfiguracionCotizacion configuracion =
                ConfiguracionCotizacion.configurar(EstrategiaCotizacion.RENDIMIENTO_ACTUAL);

        assertThatNullPointerException()
                .isThrownBy(() -> configuracion.cambiarEstrategiaDefault(null));
        assertThat(configuracion.getEstrategiaDefault())
                .isEqualTo(EstrategiaCotizacion.RENDIMIENTO_ACTUAL);
    }
}
