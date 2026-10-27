package com.tdetroy.valuacion.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * Último acumulado de temporada conocido de un jugador según la página pública de liga de WhoScored
 * (plan.md §8.1) — snapshot 1:1 por jugador, a diferencia de {@link RendimientoPartido}
 * (append-only): cada ingesta semanal reemplaza por completo el valor anterior vía {@link
 * #actualizarSnapshot}, nunca conserva historial propio. Calcular el delta entre el snapshot
 * anterior y el nuevo acumulado (actual - anterior, nunca negativo) es responsabilidad de quien
 * orquesta la ingesta ({@code RendimientoServiceImpl}), no de esta clase.
 *
 * <p>{@code metricas} guarda JSON ya serializado como texto plano, igual criterio que {@link
 * RendimientoPartido#getMetricas()}: la entidad nunca serializa, y este modelo tampoco — eso es
 * trabajo del Service que lo construye, para no acoplar {@code model/} a ninguna librería JSON
 * concreta.
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

    /**
     * Primer snapshot conocido de {@code jugadorId} — todavía sin un snapshot anterior contra el
     * que calcular un delta (quien orquesta la ingesta no persiste ningún {@link
     * RendimientoPartido} en este caso, sólo este snapshot base).
     */
    public static RendimientoAcumuladoWhoScored primeraVez(UUID jugadorId, String metricas) {
        return new RendimientoAcumuladoWhoScored(jugadorId, metricas, Instant.now());
    }

    /** Igual que {@link #primeraVez(UUID, String)}, con {@code fecha} explícita (tests). */
    public static RendimientoAcumuladoWhoScored primeraVez(
            UUID jugadorId, String metricas, Instant fecha) {
        return new RendimientoAcumuladoWhoScored(jugadorId, metricas, fecha);
    }

    /**
     * Reconstruye un snapshot ya persistido (usado por {@code
     * entity/RendimientoAcumuladoWhoScoredEntity#aModelo()} en el límite con {@code
     * repositories/}).
     */
    public static RendimientoAcumuladoWhoScored reconstruir(
            UUID jugadorId, String metricas, Instant fechaActualizacion) {
        return new RendimientoAcumuladoWhoScored(jugadorId, metricas, fechaActualizacion);
    }

    /**
     * Reemplaza el acumulado conocido por {@code nuevasMetricas} (snapshot 1:1 por jugador, nunca
     * histórico — ver javadoc de la clase).
     */
    public void actualizarSnapshot(String nuevasMetricas, Instant fecha) {
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
