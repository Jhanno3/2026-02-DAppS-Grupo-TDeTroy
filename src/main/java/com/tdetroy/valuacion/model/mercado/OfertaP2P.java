package com.tdetroy.valuacion.model.mercado;

import com.tdetroy.valuacion.common.Monetario;
import com.tdetroy.valuacion.model.jugador.Jugador;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * Oferta de venta de tokens de un {@link Jugador} entre usuarios, a un precio que fija el vendedor
 * y no cambia. Nunca se vende más que lo publicado. Mover tokens, saldo y reservas es tarea de
 * {@code OfertaService}.
 */
@Getter
public class OfertaP2P {

    private final UUID id;
    private final UUID vendedorId;
    private final UUID jugadorId;
    private final int cantidadPublicada;
    private int cantidadDisponible;
    private final BigDecimal precioUnitario;
    private EstadoOferta estado;
    private final Instant fechaCreacion;
    private final Instant fechaVencimiento;

    private OfertaP2P(
            UUID vendedorId,
            UUID jugadorId,
            int cantidadPublicada,
            BigDecimal precioUnitario,
            Instant fechaCreacion,
            Instant fechaVencimiento) {
        validarCampos(
                vendedorId,
                jugadorId,
                cantidadPublicada,
                precioUnitario,
                fechaCreacion,
                fechaVencimiento);

        this.id = UUID.randomUUID();
        this.vendedorId = vendedorId;
        this.jugadorId = jugadorId;
        this.cantidadPublicada = cantidadPublicada;
        this.cantidadDisponible = cantidadPublicada;
        this.precioUnitario = precioUnitario;
        this.estado = EstadoOferta.ABIERTA;
        this.fechaCreacion = fechaCreacion;
        this.fechaVencimiento = fechaVencimiento;
    }

    /**
     * @throws IllegalArgumentException si {@code cantidadPublicada}, {@code precioUnitario} o
     *     {@code horasLimite} no son mayores a cero
     */
    public static OfertaP2P publicar(
            UUID vendedorId,
            UUID jugadorId,
            int cantidadPublicada,
            BigDecimal precioUnitario,
            long horasLimite) {
        exigirPositivo(horasLimite, "horasLimite");
        Instant fechaCreacion = Instant.now();
        return publicar(
                vendedorId,
                jugadorId,
                cantidadPublicada,
                precioUnitario,
                fechaCreacion,
                fechaCreacion.plusSeconds(horasLimite * 3600));
    }

    public static OfertaP2P publicar(
            UUID vendedorId,
            UUID jugadorId,
            int cantidadPublicada,
            BigDecimal precioUnitario,
            Instant fechaCreacion,
            Instant fechaVencimiento) {
        return new OfertaP2P(
                vendedorId,
                jugadorId,
                cantidadPublicada,
                precioUnitario,
                fechaCreacion,
                fechaVencimiento);
    }

    private OfertaP2P(
            UUID id,
            UUID vendedorId,
            UUID jugadorId,
            int cantidadPublicada,
            int cantidadDisponible,
            BigDecimal precioUnitario,
            EstadoOferta estado,
            Instant fechaCreacion,
            Instant fechaVencimiento) {
        this.id = id;
        this.vendedorId = vendedorId;
        this.jugadorId = jugadorId;
        this.cantidadPublicada = cantidadPublicada;
        this.cantidadDisponible = cantidadDisponible;
        this.precioUnitario = precioUnitario;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
        this.fechaVencimiento = fechaVencimiento;
    }

    /** Reconstruye desde persistencia; no valida las invariantes de alta. */
    public static OfertaP2P reconstruir(
            UUID id,
            UUID vendedorId,
            UUID jugadorId,
            int cantidadPublicada,
            int cantidadDisponible,
            BigDecimal precioUnitario,
            EstadoOferta estado,
            Instant fechaCreacion,
            Instant fechaVencimiento) {
        return new OfertaP2P(
                id,
                vendedorId,
                jugadorId,
                cantidadPublicada,
                cantidadDisponible,
                precioUnitario,
                estado,
                fechaCreacion,
                fechaVencimiento);
    }

    /**
     * Una solicitud que excede lo disponible se rechaza completa, nunca se recorta.
     *
     * @return {@code cantidadSolicitada}
     * @throws IllegalArgumentException si {@code cantidadSolicitada} no es mayor a cero, o si
     *     supera lo disponible
     */
    public int ejecutarContra(int cantidadSolicitada) {
        exigirPositivo(cantidadSolicitada, "cantidadSolicitada");
        if (cantidadSolicitada > cantidadDisponible) {
            throw new IllegalArgumentException(
                    "cantidadSolicitada (%d) no puede superar cantidadDisponible actual (%d)"
                            .formatted(cantidadSolicitada, cantidadDisponible));
        }

        this.cantidadDisponible -= cantidadSolicitada;
        if (this.cantidadDisponible == 0) {
            this.estado = EstadoOferta.AGOTADA;
        }

        return cantidadSolicitada;
    }

    /**
     * No valida que esté {@link EstadoOferta#ABIERTA}: lo hace quien llama, que también libera la
     * reserva del vendedor.
     */
    public void cancelar() {
        this.cantidadDisponible = 0;
        this.estado = EstadoOferta.CANCELADA;
    }

    /** Sólo informa; no cambia el estado. */
    public boolean estaVencida(Instant ahora) {
        Objects.requireNonNull(ahora, "ahora no puede ser null");
        return ahora.isAfter(fechaVencimiento);
    }

    public BigDecimal montoPor(int cantidad) {
        return Monetario.escalar(precioUnitario.multiply(BigDecimal.valueOf(cantidad)));
    }

    private static void validarCampos(
            UUID vendedorId,
            UUID jugadorId,
            int cantidadPublicada,
            BigDecimal precioUnitario,
            Instant fechaCreacion,
            Instant fechaVencimiento) {
        Objects.requireNonNull(vendedorId, "vendedorId no puede ser null");
        Objects.requireNonNull(jugadorId, "jugadorId no puede ser null");
        Objects.requireNonNull(precioUnitario, "precioUnitario no puede ser null");
        Objects.requireNonNull(fechaCreacion, "fechaCreacion no puede ser null");
        Objects.requireNonNull(fechaVencimiento, "fechaVencimiento no puede ser null");
        exigirPositivo(cantidadPublicada, "cantidadPublicada");
        exigirPositivo(precioUnitario, "precioUnitario");
        if (!fechaVencimiento.isAfter(fechaCreacion)) {
            throw new IllegalArgumentException(
                    "fechaVencimiento (%s) debe ser posterior a fechaCreacion (%s)"
                            .formatted(fechaVencimiento, fechaCreacion));
        }
    }

    private static void exigirPositivo(int valor, String nombreCampo) {
        if (valor <= 0) {
            throw new IllegalArgumentException(
                    nombreCampo + " debe ser mayor a cero, fue " + valor);
        }
    }

    private static void exigirPositivo(long valor, String nombreCampo) {
        if (valor <= 0) {
            throw new IllegalArgumentException(
                    nombreCampo + " debe ser mayor a cero, fue " + valor);
        }
    }

    private static void exigirPositivo(BigDecimal valor, String nombreCampo) {
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException(
                    nombreCampo + " debe ser mayor a cero, fue " + valor);
        }
    }
}
