package com.tdetroy.valuacion.model.cotizacion;

import com.tdetroy.valuacion.common.Monetario;
import com.tdetroy.valuacion.model.jugador.Jugador;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.Getter;

/**
 * Cotización semanal de un {@link Jugador}, append-only. La vigente es la de {@code fechaCalculo}
 * más reciente.
 */
@Getter
public class CotizacionHistorica {

    private static final Pattern SEMANA_ISO_PATTERN = Pattern.compile("^\\d{4}-W\\d{2}$");

    private final UUID id;
    private final UUID jugadorId;
    private final String semana;
    private final BigDecimal valor;
    private final OrigenCotizacion origen;
    private final EstrategiaCotizacion estrategia;
    private final Instant fechaCalculo;

    private CotizacionHistorica(
            UUID jugadorId,
            String semana,
            BigDecimal valor,
            OrigenCotizacion origen,
            EstrategiaCotizacion estrategia,
            Instant fechaCalculo) {
        validarCampos(jugadorId, semana, valor, origen, estrategia, fechaCalculo);
        validarSemana(semana);

        this.id = UUID.randomUUID();
        this.jugadorId = jugadorId;
        this.semana = semana;
        this.valor = Monetario.escalar(valor);
        this.origen = origen;
        this.estrategia = estrategia;
        this.fechaCalculo = fechaCalculo;
    }

    private CotizacionHistorica(
            UUID id,
            UUID jugadorId,
            String semana,
            BigDecimal valor,
            OrigenCotizacion origen,
            EstrategiaCotizacion estrategia,
            Instant fechaCalculo) {
        this.id = id;
        this.jugadorId = jugadorId;
        this.semana = semana;
        this.valor = valor;
        this.origen = origen;
        this.estrategia = estrategia;
        this.fechaCalculo = fechaCalculo;
    }

    /** Usa la estrategia {@link EstrategiaCotizacion#RENDIMIENTO_TEMPORADA}. */
    public static CotizacionHistorica calcular(
            UUID jugadorId, String semana, BigDecimal valor, OrigenCotizacion origen) {
        return calcular(
                jugadorId,
                semana,
                valor,
                origen,
                EstrategiaCotizacion.RENDIMIENTO_TEMPORADA,
                Instant.now());
    }

    public static CotizacionHistorica calcular(
            UUID jugadorId,
            String semana,
            BigDecimal valor,
            OrigenCotizacion origen,
            Instant fechaCalculo) {
        return calcular(
                jugadorId,
                semana,
                valor,
                origen,
                EstrategiaCotizacion.RENDIMIENTO_TEMPORADA,
                fechaCalculo);
    }

    public static CotizacionHistorica calcular(
            UUID jugadorId,
            String semana,
            BigDecimal valor,
            OrigenCotizacion origen,
            EstrategiaCotizacion estrategia) {
        return calcular(jugadorId, semana, valor, origen, estrategia, Instant.now());
    }

    public static CotizacionHistorica calcular(
            UUID jugadorId,
            String semana,
            BigDecimal valor,
            OrigenCotizacion origen,
            EstrategiaCotizacion estrategia,
            Instant fechaCalculo) {
        return new CotizacionHistorica(jugadorId, semana, valor, origen, estrategia, fechaCalculo);
    }

    /** Reconstruye desde persistencia. */
    public static CotizacionHistorica reconstruir(
            UUID id,
            UUID jugadorId,
            String semana,
            BigDecimal valor,
            OrigenCotizacion origen,
            EstrategiaCotizacion estrategia,
            Instant fechaCalculo) {
        return new CotizacionHistorica(
                id, jugadorId, semana, valor, origen, estrategia, fechaCalculo);
    }

    /** Reconstruye con la estrategia por defecto, por compatibilidad. */
    public static CotizacionHistorica reconstruir(
            UUID id,
            UUID jugadorId,
            String semana,
            BigDecimal valor,
            OrigenCotizacion origen,
            Instant fechaCalculo) {
        return reconstruir(
                id,
                jugadorId,
                semana,
                valor,
                origen,
                EstrategiaCotizacion.RENDIMIENTO_TEMPORADA,
                fechaCalculo);
    }

    /**
     * Deja la última cotización de cada semana: recalcular una semana agrega otra fila.
     *
     * @param cronologicas cotizaciones ordenadas de la más vieja a la más nueva
     */
    public static List<CotizacionHistorica> ultimaDeCadaSemana(
            List<CotizacionHistorica> cronologicas) {
        Objects.requireNonNull(cronologicas, "cronologicas no puede ser null");
        LinkedHashMap<String, CotizacionHistorica> porSemana = new LinkedHashMap<>();
        for (CotizacionHistorica cotizacion : cronologicas) {
            porSemana.putLast(cotizacion.getSemana(), cotizacion);
        }
        return List.copyOf(porSemana.values());
    }

    private static void validarCampos(
            UUID jugadorId,
            String semana,
            BigDecimal valor,
            OrigenCotizacion origen,
            EstrategiaCotizacion estrategia,
            Instant fechaCalculo) {
        Objects.requireNonNull(jugadorId, "jugadorId no puede ser null");
        Objects.requireNonNull(semana, "semana no puede ser null");
        Objects.requireNonNull(valor, "valor no puede ser null");
        Objects.requireNonNull(origen, "origen no puede ser null");
        Objects.requireNonNull(estrategia, "estrategia no puede ser null");
        Objects.requireNonNull(fechaCalculo, "fechaCalculo no puede ser null");
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException("valor debe ser mayor a cero, fue " + valor);
        }
    }

    private static void validarSemana(String semana) {
        if (!SEMANA_ISO_PATTERN.matcher(semana).matches()) {
            throw new IllegalArgumentException(
                    "semana debe tener formato ISO week 'yyyy-Www', fue '%s'".formatted(semana));
        }
    }
}
