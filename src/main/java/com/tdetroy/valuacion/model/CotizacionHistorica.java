package com.tdetroy.valuacion.model;

import com.tdetroy.valuacion.common.Monetario;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.Getter;

/**
 * Cotización de un {@link Jugador} calculada para una semana puntual (plan.md §2.4, UC-06/UC-08/
 * UC-13).
 *
 * <p><b>Append-only:</b> no expone ningún método de mutación ni setter público — se construye
 * completo con {@link #calcular} y queda inmutable desde ese momento (constitution.md §2), mismo
 * criterio que {@link Movimiento}/{@link RendimientoPartido}. Nunca hay un {@code UPDATE} sobre un
 * registro existente: la "cotización vigente" de un jugador es, por definición, el {@code
 * CotizacionHistorica} con {@code fechaCalculo} más reciente para ese {@code jugadorId} (plan.md
 * §2.4) — una consulta sobre el historial ya persistido, nunca una fila mutable en paralelo.
 */
@Getter
public class CotizacionHistorica {

    private static final Pattern SEMANA_ISO_PATTERN = Pattern.compile("^\\d{4}-W\\d{2}$");

    private final UUID id;
    private final UUID jugadorId;
    private final String semana;
    private final BigDecimal valor;
    private final OrigenCotizacion origen;
    private final Instant fechaCalculo;

    private CotizacionHistorica(
            UUID jugadorId,
            String semana,
            BigDecimal valor,
            OrigenCotizacion origen,
            Instant fechaCalculo) {
        validarCampos(jugadorId, semana, valor, origen, fechaCalculo);
        validarSemana(semana);

        this.id = UUID.randomUUID();
        this.jugadorId = jugadorId;
        this.semana = semana;
        this.valor = Monetario.escalar(valor);
        this.origen = origen;
        this.fechaCalculo = fechaCalculo;
    }

    /** Calcula una cotización con {@code fechaCalculo = Instant.now()} (plan.md §6.1, paso 6). */
    public static CotizacionHistorica calcular(
            UUID jugadorId, String semana, BigDecimal valor, OrigenCotizacion origen) {
        return calcular(jugadorId, semana, valor, origen, Instant.now());
    }

    /**
     * Igual que {@link #calcular}, con {@code fechaCalculo} explícita (tests, reproducibilidad).
     */
    public static CotizacionHistorica calcular(
            UUID jugadorId,
            String semana,
            BigDecimal valor,
            OrigenCotizacion origen,
            Instant fechaCalculo) {
        return new CotizacionHistorica(jugadorId, semana, valor, origen, fechaCalculo);
    }

    private CotizacionHistorica(
            UUID id,
            UUID jugadorId,
            String semana,
            BigDecimal valor,
            OrigenCotizacion origen,
            Instant fechaCalculo) {
        this.id = id;
        this.jugadorId = jugadorId;
        this.semana = semana;
        this.valor = valor;
        this.origen = origen;
        this.fechaCalculo = fechaCalculo;
    }

    /**
     * Reconstruye una {@code CotizacionHistorica} ya persistida a partir de sus datos crudos (usado
     * por {@code entity/CotizacionHistoricaEntity#aModelo()} en el límite con {@code
     * repositories/}) — a diferencia de {@link #calcular}, no valida las invariantes de alta.
     */
    public static CotizacionHistorica reconstruir(
            UUID id,
            UUID jugadorId,
            String semana,
            BigDecimal valor,
            OrigenCotizacion origen,
            Instant fechaCalculo) {
        return new CotizacionHistorica(id, jugadorId, semana, valor, origen, fechaCalculo);
    }

    private static void validarCampos(
            UUID jugadorId,
            String semana,
            BigDecimal valor,
            OrigenCotizacion origen,
            Instant fechaCalculo) {
        Objects.requireNonNull(jugadorId, "jugadorId no puede ser null");
        Objects.requireNonNull(semana, "semana no puede ser null");
        Objects.requireNonNull(valor, "valor no puede ser null");
        Objects.requireNonNull(origen, "origen no puede ser null");
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
