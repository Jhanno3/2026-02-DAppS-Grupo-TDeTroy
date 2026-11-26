package com.tdetroy.valuacion.model.mercado;

import com.tdetroy.valuacion.common.Monetario;
import com.tdetroy.valuacion.common.exceptions.OperacionSubastaInvalidaException;
import com.tdetroy.valuacion.model.jugador.Jugador;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Subasta de tokens de un {@link Jugador}. {@code fechaVencimiento} es el plazo para la primera
 * puja; cada puja reinicia {@code fechaCierre} a {@link #ESPERA_ENTRE_PUJAS}, aunque todavía quede
 * duración. Mover tokens, saldo y reservas es tarea de {@code SubastaService}.
 */
@Getter
// Constructor con todos los campos en orden de declaración, sólo para crear() y reconstruir().
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Subasta {

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

    /** Reconstruye desde persistencia; no valida las invariantes de alta. */
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
     * Precio por token al pujar, calculado sobre el vigente o, sin pujas, sobre el inicial.
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

    public BigDecimal montoTotal(BigDecimal precioUnitario) {
        return Monetario.escalar(precioUnitario.multiply(BigDecimal.valueOf(cantidad)));
    }

    /** Monto reservado por el mejor postor; cero si no hubo pujas. */
    public BigDecimal montoComprometido() {
        return tienePujas() ? montoTotal(precioActual) : Monetario.CERO;
    }

    public Instant cierraEn() {
        return tienePujas() ? fechaCierre : fechaVencimiento;
    }

    /**
     * Actualiza precio y mejor postor, y reinicia {@code fechaCierre}.
     *
     * @return el nuevo precio por token
     * @throws OperacionSubastaInvalidaException si la subasta no está activa o ya cerró, si {@code
     *     postorId} es el vendedor o ya es el mejor postor, o si el incremento no aplica
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

    public boolean debeAdjudicarse(Instant ahora) {
        return estado == EstadoSubasta.ACTIVA && tienePujas() && !ahora.isBefore(fechaCierre);
    }

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
     * Liberar los tokens y devolver el saldo del mejor postor es tarea de {@code SubastaService}.
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
