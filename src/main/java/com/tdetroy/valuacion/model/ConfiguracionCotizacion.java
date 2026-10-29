package com.tdetroy.valuacion.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * Configuración del motor de cotización ajustable en runtime por un {@code ADMIN}: hoy, sólo la
 * estrategia por defecto con la que calculan el job semanal y el recálculo manual sin estrategia
 * explícita. Es un registro único ({@link #ID}): mientras nadie la cambie no existe la fila, y vale
 * la estrategia de {@code app.cotizacion.estrategia-default}.
 */
@Getter
public class ConfiguracionCotizacion {

    /** Id fijo del registro único, también usado como {@code entidadId} en la auditoría. */
    public static final UUID ID = UUID.fromString("00000000-0000-0000-0000-00000000c071");

    private EstrategiaCotizacion estrategiaDefault;
    private Instant fechaActualizacion;

    private ConfiguracionCotizacion(
            EstrategiaCotizacion estrategiaDefault, Instant fechaActualizacion) {
        this.estrategiaDefault =
                Objects.requireNonNull(estrategiaDefault, "estrategiaDefault no puede ser null");
        this.fechaActualizacion =
                Objects.requireNonNull(fechaActualizacion, "fechaActualizacion no puede ser null");
    }

    public static ConfiguracionCotizacion configurar(EstrategiaCotizacion estrategiaDefault) {
        return new ConfiguracionCotizacion(estrategiaDefault, Instant.now());
    }

    public static ConfiguracionCotizacion reconstruir(
            EstrategiaCotizacion estrategiaDefault, Instant fechaActualizacion) {
        return new ConfiguracionCotizacion(estrategiaDefault, fechaActualizacion);
    }

    public void cambiarEstrategiaDefault(EstrategiaCotizacion nuevaEstrategia) {
        this.estrategiaDefault =
                Objects.requireNonNull(nuevaEstrategia, "nuevaEstrategia no puede ser null");
        this.fechaActualizacion = Instant.now();
    }
}
