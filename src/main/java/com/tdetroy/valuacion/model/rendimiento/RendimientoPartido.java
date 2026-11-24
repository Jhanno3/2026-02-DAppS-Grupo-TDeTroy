package com.tdetroy.valuacion.model.rendimiento;

import com.tdetroy.valuacion.model.jugador.Jugador;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.Getter;

/**
 * Rendimiento crudo de un {@link Jugador} en un partido, append-only. Guarda todas las métricas de
 * la fuente: el motor de cotización decide cuáles pesan. {@code partidoExternoId} y {@code
 * jugadorId} evitan duplicar una reingesta.
 *
 * <p>{@code metricas} es JSON ya serializado, para no acoplar {@code model/} a una librería JSON.
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

    /** Reconstruye desde persistencia; no valida las invariantes de alta. */
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
        exigirNoVacio(partidoExternoId, "partidoExternoId");
        exigirNoVacio(metricas, "metricas");
    }

    private static void validarSemanaCalculo(String semanaCalculo) {
        if (!SEMANA_ISO_PATTERN.matcher(semanaCalculo).matches()) {
            throw new IllegalArgumentException(
                    "semanaCalculo debe tener formato ISO week 'yyyy-Www', fue '%s'"
                            .formatted(semanaCalculo));
        }
    }

    private static void exigirNoVacio(String valor, String nombreCampo) {
        if (valor.isBlank()) {
            throw new IllegalArgumentException(nombreCampo + " no puede estar vacío ni ser blanco");
        }
    }
}
