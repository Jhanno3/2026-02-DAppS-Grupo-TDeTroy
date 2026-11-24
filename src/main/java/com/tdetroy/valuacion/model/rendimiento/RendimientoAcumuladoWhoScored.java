package com.tdetroy.valuacion.model.rendimiento;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * Último acumulado de temporada de un jugador: cada ingesta lo reemplaza, sin historial. {@code
 * metricas} es JSON ya serializado, para no acoplar {@code model/} a una librería JSON.
 */
@Getter
public class RendimientoAcumuladoWhoScored {

    private final UUID jugadorId;
    private String metricas;
    private Instant fechaActualizacion;

    private RendimientoAcumuladoWhoScored(
            UUID jugadorId, String metricas, Instant fechaActualizacion) {
        validar(jugadorId, metricas, fechaActualizacion);
        this.jugadorId = jugadorId;
        this.metricas = metricas;
        this.fechaActualizacion = fechaActualizacion;
    }

    /** Primer snapshot: no hay anterior contra el que calcular un delta. */
    public static RendimientoAcumuladoWhoScored primeraVez(UUID jugadorId, String metricas) {
        return new RendimientoAcumuladoWhoScored(jugadorId, metricas, Instant.now());
    }

    public static RendimientoAcumuladoWhoScored primeraVez(
            UUID jugadorId, String metricas, Instant fecha) {
        return new RendimientoAcumuladoWhoScored(jugadorId, metricas, fecha);
    }

    /** Reconstruye desde persistencia. */
    public static RendimientoAcumuladoWhoScored reconstruir(
            UUID jugadorId, String metricas, Instant fechaActualizacion) {
        return new RendimientoAcumuladoWhoScored(jugadorId, metricas, fechaActualizacion);
    }

    public void actualizarAcumulado(String nuevasMetricas, Instant fecha) {
        validarMetricas(nuevasMetricas);
        Objects.requireNonNull(fecha, "fecha no puede ser null");
        this.metricas = nuevasMetricas;
        this.fechaActualizacion = fecha;
    }

    private static void validar(UUID jugadorId, String metricas, Instant fechaActualizacion) {
        Objects.requireNonNull(jugadorId, "jugadorId no puede ser null");
        Objects.requireNonNull(fechaActualizacion, "fechaActualizacion no puede ser null");
        validarMetricas(metricas);
    }

    private static void validarMetricas(String metricas) {
        Objects.requireNonNull(metricas, "metricas no puede ser null");
        if (metricas.isBlank()) {
            throw new IllegalArgumentException("metricas no puede estar vacío ni ser blanco");
        }
    }
}
