package com.tdetroy.valuacion.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * Oferta abierta de venta directa entre usuarios de una cantidad de tokens de un {@link Jugador}
 * (plan.md §2.7, UC-17). El vendedor publica una cantidad que posee como tenencia no reservada
 * ({@link TenenciaToken#reservar}, responsabilidad de {@code OfertaService}, no de esta clase), a
 * un {@code precioUnitario} que el propio vendedor fija libremente al publicar — fijo durante toda
 * la vida de la oferta, nunca recalculado contra la cotización vigente del jugador ni en ningún
 * otro momento. Cualquier otro usuario interesado puede comprar hasta {@code cantidadDisponible} a
 * ese mismo precio publicado.
 *
 * <p>Si nadie la compra (del todo o en parte) antes de {@code fechaVencimiento}, la oferta vence:
 * {@code OfertaService} (vía un chequeo periódico, no esta clase) la cancela automáticamente y
 * reintegra lo no vendido a la tenencia del vendedor, igual mecanismo que una cancelación
 * voluntaria (ver {@link #cancelar()}).
 *
 * <p>{@code cantidadDisponible}/{@code estado} nunca se exponen con un setter público: sólo se
 * mueven a través de {@link #ejecutarContra(int)} (constitution.md §2, mismo criterio que la
 * invariante {@code cantidadReservada ≤ cantidad} en {@link TenenciaToken}) — "nunca se vende más
 * cantidad que la publicada" (spec.md UC-17) vive en esta clase, no sólo en {@code OfertaService}.
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
     * Publica una oferta {@link EstadoOferta#ABIERTA} por {@code cantidadPublicada} tokens de
     * {@code jugadorId} que {@code vendedorId} posee, a {@code precioUnitario} (fijado libremente
     * por el vendedor, fijo durante toda la vida de la oferta), con {@code fechaCreacion =
     * Instant.now()} y {@code fechaVencimiento = fechaCreacion + horasLimite} (UC-17). Único punto
     * de creación de {@code OfertaP2P} — reservar esa cantidad en la {@link TenenciaToken} del
     * vendedor es responsabilidad de {@code OfertaService}, no de este método.
     *
     * @throws IllegalArgumentException si {@code cantidadPublicada}, {@code precioUnitario} o
     *     {@code horasLimite} no son mayores a cero
     */
    public static OfertaP2P publicar(
            UUID vendedorId,
            UUID jugadorId,
            int cantidadPublicada,
            BigDecimal precioUnitario,
            long horasLimite) {
        requirePositivo(horasLimite, "horasLimite");
        Instant fechaCreacion = Instant.now();
        return publicar(
                vendedorId,
                jugadorId,
                cantidadPublicada,
                precioUnitario,
                fechaCreacion,
                fechaCreacion.plusSeconds(horasLimite * 3600));
    }

    /**
     * Igual que {@link #publicar(UUID, UUID, int, BigDecimal, long)}, con {@code fechaCreacion}/
     * {@code fechaVencimiento} explícitas (tests, reproducibilidad) — mismo criterio que {@link
     * Usuario#registrar(String, String, Instant)}.
     */
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

    /**
     * Reconstruye una {@code OfertaP2P} ya persistida a partir de sus datos crudos (usado por
     * {@code entity/OfertaP2PEntity#aModelo()} en el límite con {@code repositories/}) — a
     * diferencia de {@link #publicar}, no valida ni aplica defaults de alta.
     */
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
     * Ejecuta una compra de {@code cantidadSolicitada} contra esta oferta (UC-17), siempre a {@link
     * #getPrecioUnitario()}: nunca ejecuta más que {@link #getCantidadDisponible()} — a diferencia
     * de una venta parcial silenciosa, una solicitud que excede lo disponible se rechaza por
     * completo, nunca se recorta. Si {@code cantidadDisponible} llega a cero, pasa a {@link
     * EstadoOferta#AGOTADA}. Debitar la reserva del vendedor, acreditar al comprador y registrar
     * los movimientos es responsabilidad de {@code OfertaService}, no de este método — acá sólo se
     * protege la invariante de cantidad de la propia oferta.
     *
     * @return {@code cantidadSolicitada}, tal cual se ejecutó (nunca hay ejecución parcial)
     * @throws IllegalArgumentException si {@code cantidadSolicitada} no es mayor a cero, o si
     *     supera {@code cantidadDisponible} actual
     */
    public int ejecutarContra(int cantidadSolicitada) {
        requirePositivo(cantidadSolicitada, "cantidadSolicitada");
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
     * Cancela la oferta (UC-17, T6.5/T6.6, y por vencimiento automático): {@code
     * cantidadDisponible} pasa a cero y {@code estado} a {@link EstadoOferta#CANCELADA}, sin
     * importar cuánto quedara disponible. Sólo mueve estos dos campos propios — liberar la reserva
     * correspondiente en la {@link TenenciaToken} del vendedor (la cantidad que tenía disponible al
     * momento de cancelar) es responsabilidad de {@code OfertaService}, no de este método (mismo
     * reparto de responsabilidades que {@link #ejecutarContra(int)}). Validar que la oferta esté
     * {@link EstadoOferta#ABIERTA} antes de invocarlo es responsabilidad de quien llama — mismo
     * criterio sin guarda interna que {@link Jugador#darDeBaja()}: acá no hay una invariante
     * numérica propia que proteger, sólo una transición de estado que el Service ya validó
     * (cancelación voluntaria, T6.5; vencimiento automático; cancelación automática por baja de
     * jugador, T6.6) porque sólo itera ofertas {@code ABIERTA} en cada uno de esos tres casos.
     */
    public void cancelar() {
        this.cantidadDisponible = 0;
        this.estado = EstadoOferta.CANCELADA;
    }

    /**
     * {@code true} si esta oferta ya pasó su {@code fechaVencimiento} al momento {@code ahora}
     * (UC-17: "si pasa esa cantidad de horas sin venderse, se cancela la oferta"). No mueve ningún
     * estado por sí sola — sólo informa; cancelarla es responsabilidad de quien la invoque
     * periódicamente (el chequeo de vencimiento de {@code OfertaService}, nunca esta clase).
     */
    public boolean estaVencida(Instant ahora) {
        Objects.requireNonNull(ahora, "ahora no puede ser null");
        return ahora.isAfter(fechaVencimiento);
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
        requirePositivo(cantidadPublicada, "cantidadPublicada");
        requirePositivo(precioUnitario, "precioUnitario");
        if (!fechaVencimiento.isAfter(fechaCreacion)) {
            throw new IllegalArgumentException(
                    "fechaVencimiento (%s) debe ser posterior a fechaCreacion (%s)"
                            .formatted(fechaVencimiento, fechaCreacion));
        }
    }

    private static void requirePositivo(int valor, String nombreCampo) {
        if (valor <= 0) {
            throw new IllegalArgumentException(
                    nombreCampo + " debe ser mayor a cero, fue " + valor);
        }
    }

    private static void requirePositivo(long valor, String nombreCampo) {
        if (valor <= 0) {
            throw new IllegalArgumentException(
                    nombreCampo + " debe ser mayor a cero, fue " + valor);
        }
    }

    private static void requirePositivo(BigDecimal valor, String nombreCampo) {
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException(
                    nombreCampo + " debe ser mayor a cero, fue " + valor);
        }
    }
}
