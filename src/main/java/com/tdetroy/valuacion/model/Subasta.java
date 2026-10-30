package com.tdetroy.valuacion.model;

import com.tdetroy.valuacion.common.Monetario;
import com.tdetroy.valuacion.common.exceptions.OperacionSubastaInvalidaException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Subasta de una cantidad de tokens de un {@link Jugador} entre usuarios (UC-18). El vendedor fija
 * al crearla la {@code cantidad}, el {@code precioInicial} por token y la duración ({@code
 * fechaVencimiento}, plazo para la primera puja). Cada puja sube el precio vigente en un {@link
 * IncrementoPuja} fijo — nunca un monto libre — y reinicia {@code fechaCierre} a {@link
 * #ESPERA_ENTRE_PUJAS} desde ese momento: la subasta se adjudica al mejor postor cuando pasa {@code
 * fechaCierre} sin pujas nuevas, aunque todavía quede duración.
 *
 * <p>Esta clase sólo protege las reglas propias de la subasta (quién puede pujar, cuánto, hasta
 * cuándo, transiciones de estado). Reservar los tokens del vendedor, reservar/devolver el saldo de
 * los postores y mover tokens y saldo al adjudicar es responsabilidad de {@code SubastaService}
 * (mismo reparto que {@link OfertaP2P}).
 */
@Getter
// Constructor con todos los campos en orden de declaración, sólo para crear() y reconstruir().
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Subasta {

    /** Plazo sin pujas nuevas, desde la última puja, tras el cual la subasta se adjudica. */
    public static final Duration ESPERA_ENTRE_PUJAS = Duration.ofMinutes(2);

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final UUID id;
    private final UUID vendedorId;
    private final UUID jugadorId;
    private final int cantidad;
    private final BigDecimal precioInicial;
    private BigDecimal precioActual;
    private UUID mejorPostorId;
    private int cantidadPujas;
    private EstadoSubasta estado;
    private final Instant fechaCreacion;
    private final Instant fechaVencimiento;
    private Instant fechaCierre;

    /**
     * Crea una subasta {@link EstadoSubasta#ACTIVA} sin pujas, con {@code fechaVencimiento =
     * fechaCreacion + duracion}.
     *
     * @throws IllegalArgumentException si {@code cantidad}, {@code precioInicial} o {@code
     *     duracion} no son mayores a cero
     */
    public static Subasta crear(
            UUID vendedorId,
            UUID jugadorId,
            int cantidad,
            BigDecimal precioInicial,
            Duration duracion,
            Instant fechaCreacion) {
        Objects.requireNonNull(vendedorId, "vendedorId no puede ser null");
        Objects.requireNonNull(jugadorId, "jugadorId no puede ser null");
        Objects.requireNonNull(precioInicial, "precioInicial no puede ser null");
        Objects.requireNonNull(duracion, "duracion no puede ser null");
        Objects.requireNonNull(fechaCreacion, "fechaCreacion no puede ser null");
        if (cantidad <= 0) {
            throw new IllegalArgumentException("cantidad debe ser mayor a cero, fue " + cantidad);
        }
        if (precioInicial.signum() <= 0) {
            throw new IllegalArgumentException(
                    "precioInicial debe ser mayor a cero, fue " + precioInicial);
        }
        if (duracion.isNegative() || duracion.isZero()) {
            throw new IllegalArgumentException("duracion debe ser mayor a cero, fue " + duracion);
        }
        return new Subasta(
                UUID.randomUUID(),
                vendedorId,
                jugadorId,
                cantidad,
                Monetario.escalar(precioInicial),
                null,
                null,
                0,
                EstadoSubasta.ACTIVA,
                fechaCreacion,
                fechaCreacion.plus(duracion),
                null);
    }

    /**
     * Reconstruye una subasta ya persistida (usado por {@code entity/SubastaEntity#aModelo()}); no
     * valida ni aplica defaults de alta.
     */
    public static Subasta reconstruir(
            UUID id,
            UUID vendedorId,
            UUID jugadorId,
            int cantidad,
            BigDecimal precioInicial,
            BigDecimal precioActual,
            UUID mejorPostorId,
            int cantidadPujas,
            EstadoSubasta estado,
            Instant fechaCreacion,
            Instant fechaVencimiento,
            Instant fechaCierre) {
        return new Subasta(
                id,
                vendedorId,
                jugadorId,
                cantidad,
                precioInicial,
                precioActual,
                mejorPostorId,
                cantidadPujas,
                estado,
                fechaCreacion,
                fechaVencimiento,
                fechaCierre);
    }

    public boolean tienePujas() {
        return mejorPostorId != null;
    }

    /**
     * Precio por token que resultaría de pujar con {@code incremento} sobre el precio vigente (el
     * inicial si todavía no hubo pujas), ya escalado con {@link Monetario}.
     *
     * @throws OperacionSubastaInvalidaException si {@code incremento} es {@link
     *     IncrementoPuja#PRECIO_INICIAL} y ya hubo pujas
     */
    public BigDecimal precioConIncremento(IncrementoPuja incremento) {
        Objects.requireNonNull(incremento, "incremento no puede ser null");
        if (incremento == IncrementoPuja.PRECIO_INICIAL) {
            if (tienePujas()) {
                throw new OperacionSubastaInvalidaException(
                        "Sólo la primera puja puede igualar el precio inicial; ahora hay que subir"
                                + " 5, 10 o 20%");
            }
            return precioInicial;
        }
        BigDecimal base = tienePujas() ? precioActual : precioInicial;
        BigDecimal factor =
                BigDecimal.ONE.add(
                        BigDecimal.valueOf(incremento.getPorcentaje())
                                .divide(CIEN, 4, Monetario.REDONDEO));
        return Monetario.escalar(base.multiply(factor));
    }

    /** Monto total (precio × cantidad) que paga quien puje con {@code precioUnitario}. */
    public BigDecimal montoTotal(BigDecimal precioUnitario) {
        return Monetario.escalar(precioUnitario.multiply(BigDecimal.valueOf(cantidad)));
    }

    /** Monto reservado por el mejor postor actual; cero si todavía no hubo pujas. */
    public BigDecimal montoComprometido() {
        return tienePujas() ? montoTotal(precioActual) : Monetario.CERO;
    }

    /**
     * Momento en que la subasta deja de aceptar pujas: {@code fechaCierre} si ya hubo pujas, {@code
     * fechaVencimiento} si no.
     */
    public Instant cierraEn() {
        return tienePujas() ? fechaCierre : fechaVencimiento;
    }

    /**
     * Registra una puja de {@code postorId} con {@code incremento} en {@code ahora}: actualiza el
     * precio vigente y el mejor postor, y reinicia {@code fechaCierre} a {@code ahora + 2 minutos}.
     *
     * @return el nuevo precio por token
     * @throws OperacionSubastaInvalidaException si la subasta no está activa o ya cerró en {@code
     *     ahora}, si {@code postorId} es el vendedor o ya es el mejor postor, o si el incremento no
     *     aplica
     */
    public BigDecimal pujar(UUID postorId, IncrementoPuja incremento, Instant ahora) {
        Objects.requireNonNull(postorId, "postorId no puede ser null");
        Objects.requireNonNull(ahora, "ahora no puede ser null");
        if (estado != EstadoSubasta.ACTIVA || !ahora.isBefore(cierraEn())) {
            throw new OperacionSubastaInvalidaException(
                    "La subasta %s ya no acepta pujas".formatted(id));
        }
        if (postorId.equals(vendedorId)) {
            throw new OperacionSubastaInvalidaException("No podés pujar en tu propia subasta");
        }
        if (postorId.equals(mejorPostorId)) {
            throw new OperacionSubastaInvalidaException("Ya sos el mejor postor de esta subasta");
        }
        BigDecimal nuevoPrecio = precioConIncremento(incremento);
        this.precioActual = nuevoPrecio;
        this.mejorPostorId = postorId;
        this.cantidadPujas++;
        this.fechaCierre = ahora.plus(ESPERA_ENTRE_PUJAS);
        return nuevoPrecio;
    }

    /** {@code true} si está activa, tuvo pujas y ya pasaron 2 minutos desde la última. */
    public boolean debeAdjudicarse(Instant ahora) {
        return estado == EstadoSubasta.ACTIVA && tienePujas() && !ahora.isBefore(fechaCierre);
    }

    /** {@code true} si está activa, nunca tuvo pujas y ya venció su duración. */
    public boolean quedoDesierta(Instant ahora) {
        return estado == EstadoSubasta.ACTIVA && !tienePujas() && !ahora.isBefore(fechaVencimiento);
    }

    /**
     * @throws IllegalStateException si no corresponde adjudicarla en {@code ahora}
     */
    public void adjudicar(Instant ahora) {
        if (!debeAdjudicarse(ahora)) {
            throw new IllegalStateException("La subasta %s no puede adjudicarse".formatted(id));
        }
        this.estado = EstadoSubasta.ADJUDICADA;
    }

    /**
     * @throws IllegalStateException si no quedó desierta en {@code ahora}
     */
    public void declararDesierta(Instant ahora) {
        if (!quedoDesierta(ahora)) {
            throw new IllegalStateException(
                    "La subasta %s no puede declararse desierta".formatted(id));
        }
        this.estado = EstadoSubasta.DESIERTA;
    }

    /**
     * Cancela una subasta activa (por el vendedor o por baja del jugador). Liberar los tokens y
     * devolver el saldo del mejor postor es responsabilidad de {@code SubastaService}.
     *
     * @throws OperacionSubastaInvalidaException si la subasta ya no está activa
     */
    public void cancelar() {
        if (estado != EstadoSubasta.ACTIVA) {
            throw new OperacionSubastaInvalidaException(
                    "La subasta %s ya no está activa (estado actual: %s)".formatted(id, estado));
        }
        this.estado = EstadoSubasta.CANCELADA;
    }
}
