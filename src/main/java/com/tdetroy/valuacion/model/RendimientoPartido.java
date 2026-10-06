package com.tdetroy.valuacion.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.Getter;

/**
 * Rendimiento crudo de un {@link Jugador} en un partido puntual (UC-05, plan.md §2.3) — insumo de
 * la ingesta semanal, no un resultado ya curado: {@code metricas} guarda TODAS las métricas que
 * WhoScored entrega para ese jugador/partido sin recorte previo (spec.md UC-05), para que el motor
 * de cotización (plan.md §6) decida qué claves pesan sin depender de una migración de esquema cada
 * vez que la fuente agregue o quite un campo.
 *
 * <p><b>Append-only:</b> no expone ningún método de mutación ni setter público — se construye
 * completo con {@link #ingestar} y queda inmutable desde ese momento (constitution.md §2), mismo
 * criterio que {@link Movimiento}/{@code RegistroAuditoria}. {@code partidoExternoId} (id del
 * partido según Football-Data.org) combinado con {@code jugadorId} es la clave de deduplicación
 * frente a una reingesta del mismo ciclo semanal.
 *
 * <p>{@code metricas} guarda JSON ya serializado como texto plano, mapeado a una columna {@code
 * jsonb} por {@code entity/RendimientoPartidoEntity} — la entidad nunca serializa, eso es trabajo
 * del Service que la construya (mismo patrón que {@code RegistroAuditoria.valoresDespues}), para no
 * acoplar {@code model/} a ninguna librería JSON concreta.
 */
@Getter
public class RendimientoPartido {

    private static final Pattern SEMANA_ISO_PATTERN = Pattern.compile("^\\d{4}-W\\d{2}$");

    private final UUID id;
    private final UUID jugadorId;
    private final String partidoExternoId;
    private final LocalDate fechaPartido;
    private final String semanaCalculo;
    private final String metricas;
    private final FuenteResultado fuenteResultado;
    private final Instant fechaIngesta;

    private RendimientoPartido(
            UUID jugadorId,
            String partidoExternoId,
            LocalDate fechaPartido,
            String semanaCalculo,
            String metricas,
            FuenteResultado fuenteResultado,
            Instant fechaIngesta) {
        validarCampos(
                jugadorId,
                partidoExternoId,
                fechaPartido,
                semanaCalculo,
                metricas,
                fuenteResultado,
                fechaIngesta);
        validarSemanaCalculo(semanaCalculo);

        this.id = UUID.randomUUID();
        this.jugadorId = jugadorId;
        this.partidoExternoId = partidoExternoId;
        this.fechaPartido = fechaPartido;
        this.semanaCalculo = semanaCalculo;
        this.metricas = metricas;
        this.fuenteResultado = fuenteResultado;
        this.fechaIngesta = fechaIngesta;
    }

    /** Ingesta un rendimiento con {@code fechaIngesta = Instant.now()}. */
    public static RendimientoPartido ingestar(
            UUID jugadorId,
            String partidoExternoId,
            LocalDate fechaPartido,
            String semanaCalculo,
            String metricas,
            FuenteResultado fuenteResultado) {
        return ingestar(
                jugadorId,
                partidoExternoId,
                fechaPartido,
                semanaCalculo,
                metricas,
                fuenteResultado,
                Instant.now());
    }

    /**
     * Igual que {@link #ingestar}, con {@code fechaIngesta} explícita (tests, reproducibilidad).
     */
    public static RendimientoPartido ingestar(
            UUID jugadorId,
            String partidoExternoId,
            LocalDate fechaPartido,
            String semanaCalculo,
            String metricas,
            FuenteResultado fuenteResultado,
            Instant fechaIngesta) {
        return new RendimientoPartido(
                jugadorId,
                partidoExternoId,
                fechaPartido,
                semanaCalculo,
                metricas,
                fuenteResultado,
                fechaIngesta);
    }

    private RendimientoPartido(
            UUID id,
            UUID jugadorId,
            String partidoExternoId,
            LocalDate fechaPartido,
            String semanaCalculo,
            String metricas,
            FuenteResultado fuenteResultado,
            Instant fechaIngesta) {
        this.id = id;
        this.jugadorId = jugadorId;
        this.partidoExternoId = partidoExternoId;
        this.fechaPartido = fechaPartido;
        this.semanaCalculo = semanaCalculo;
        this.metricas = metricas;
        this.fuenteResultado = fuenteResultado;
        this.fechaIngesta = fechaIngesta;
    }

    /**
     * Reconstruye un {@code RendimientoPartido} ya persistido a partir de sus datos crudos (usado
     * por {@code entity/RendimientoPartidoEntity#aModelo()} en el límite con {@code repositories/})
     * — a diferencia de {@link #ingestar}, no valida las invariantes de alta.
     */
    public static RendimientoPartido reconstruir(
            UUID id,
            UUID jugadorId,
            String partidoExternoId,
            LocalDate fechaPartido,
            String semanaCalculo,
            String metricas,
            FuenteResultado fuenteResultado,
            Instant fechaIngesta) {
        return new RendimientoPartido(
                id,
                jugadorId,
                partidoExternoId,
                fechaPartido,
                semanaCalculo,
                metricas,
                fuenteResultado,
                fechaIngesta);
    }

    private static void validarCampos(
            UUID jugadorId,
            String partidoExternoId,
            LocalDate fechaPartido,
            String semanaCalculo,
            String metricas,
            FuenteResultado fuenteResultado,
            Instant fechaIngesta) {
        Objects.requireNonNull(jugadorId, "jugadorId no puede ser null");
        Objects.requireNonNull(partidoExternoId, "partidoExternoId no puede ser null");
        Objects.requireNonNull(fechaPartido, "fechaPartido no puede ser null");
        Objects.requireNonNull(semanaCalculo, "semanaCalculo no puede ser null");
        Objects.requireNonNull(metricas, "metricas no puede ser null");
        Objects.requireNonNull(fuenteResultado, "fuenteResultado no puede ser null");
        Objects.requireNonNull(fechaIngesta, "fechaIngesta no puede ser null");
        requireNoBlank(partidoExternoId, "partidoExternoId");
        requireNoBlank(metricas, "metricas");
    }

    private static void validarSemanaCalculo(String semanaCalculo) {
        if (!SEMANA_ISO_PATTERN.matcher(semanaCalculo).matches()) {
            throw new IllegalArgumentException(
                    "semanaCalculo debe tener formato ISO week 'yyyy-Www', fue '%s'"
                            .formatted(semanaCalculo));
        }
    }

    private static void requireNoBlank(String valor, String nombreCampo) {
        if (valor.isBlank()) {
            throw new IllegalArgumentException(nombreCampo + " no puede estar vacío ni ser blanco");
        }
    }
}
