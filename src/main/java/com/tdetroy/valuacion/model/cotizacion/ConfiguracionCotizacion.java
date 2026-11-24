package com.tdetroy.valuacion.model.cotizacion;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * Estrategia por defecto, ajustable en runtime. Registro único: si no existe la fila, vale {@code
 * app.cotizacion.estrategia-default}.
 */
@Getter
public class ConfiguracionCotizacion {

    /** Id fijo del registro único, también usado como {@code entidadId} en la auditoría. */
    public static final UUID ID = UUID.fromString("00000000-0000-0000-0000-00000000c071");

    private EstrategiaCotizacion estrategiaPorDefecto;
    private Instant fechaActualizacion;

    private ConfiguracionCotizacion(
            EstrategiaCotizacion estrategiaPorDefecto, Instant fechaActualizacion) {
        this.estrategiaPorDefecto =
                Objects.requireNonNull(
                        estrategiaPorDefecto, "estrategiaPorDefecto no puede ser null");
        this.fechaActualizacion =
                Objects.requireNonNull(fechaActualizacion, "fechaActualizacion no puede ser null");
    }

    public static ConfiguracionCotizacion configurar(EstrategiaCotizacion estrategiaPorDefecto) {
        return new ConfiguracionCotizacion(estrategiaPorDefecto, Instant.now());
    }

    public static ConfiguracionCotizacion reconstruir(
            EstrategiaCotizacion estrategiaPorDefecto, Instant fechaActualizacion) {
        return new ConfiguracionCotizacion(estrategiaPorDefecto, fechaActualizacion);
    }

    public void cambiarEstrategiaPorDefecto(EstrategiaCotizacion nuevaEstrategia) {
        this.estrategiaPorDefecto =
                Objects.requireNonNull(nuevaEstrategia, "nuevaEstrategia no puede ser null");
        this.fechaActualizacion = Instant.now();
    }
}
