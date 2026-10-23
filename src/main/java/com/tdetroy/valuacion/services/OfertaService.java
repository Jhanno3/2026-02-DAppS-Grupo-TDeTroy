package com.tdetroy.valuacion.services;

import com.tdetroy.valuacion.common.exceptions.OfertaInsuficienteException;
import com.tdetroy.valuacion.common.exceptions.OfertaNoAbiertaException;
import com.tdetroy.valuacion.common.exceptions.OfertaNoPerteneceAlVendedorException;
import com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException;
import com.tdetroy.valuacion.common.exceptions.SaldoInsuficienteException;
import com.tdetroy.valuacion.common.exceptions.TenenciaInsuficienteException;
import com.tdetroy.valuacion.model.Movimiento;
import com.tdetroy.valuacion.model.OfertaP2P;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Venta directa entre usuarios mediante oferta abierta (plan.md §1, §9, UC-17) — convive con la
 * compra/venta contra el sistema ({@code TokenService}, UC-09/UC-10) sin reemplazarla ni limitarla.
 * A diferencia de esa vía (y de la cotización oficial en general), acá el precio lo fija libremente
 * el propio vendedor al publicar la oferta, fijo durante toda su vida — este Service nunca lo
 * recalcula contra la cotización vigente del jugador.
 */
public interface OfertaService {

    /**
     * Publica una oferta {@code ABIERTA} por {@code cantidad} tokens de {@code jugadorId} que
     * {@code vendedorId} posee como tenencia no reservada en otra oferta, a {@code precioUnitario}
     * (fijado libremente por el vendedor) y con un límite de {@code horasLimite} horas antes de
     * vencer automáticamente (UC-17, plan.md §9.1): reserva esa cantidad en su {@code
     * TenenciaToken} y crea la oferta.
     *
     * @throws TenenciaInsuficienteException si {@code cantidad} supera la tenencia disponible (no
     *     reservada) del vendedor para ese jugador
     * @throws RecursoNoEncontradoException si no existe un jugador con {@code jugadorId}
     * @throws IllegalArgumentException si {@code cantidad}, {@code precioUnitario} o {@code
     *     horasLimite} no son mayores a cero
     */
    OfertaP2P publicar(
            UUID vendedorId,
            UUID jugadorId,
            int cantidad,
            BigDecimal precioUnitario,
            long horasLimite);

    /** Ofertas {@code ABIERTA} de un jugador, para que un comprador elija (UC-17). */
    List<OfertaP2P> listarAbiertasDe(UUID jugadorId);

    /**
     * Ofertas {@code ABIERTA} de todo el catálogo, sin filtrar por jugador (UC-17, "pestaña P2P").
     */
    List<OfertaP2P> listarAbiertas();

    /**
     * Ejecuta una compra de {@code cantidad} contra la oferta {@code ofertaId} (UC-17, plan.md
     * §9.2), siempre al {@code precioUnitario} que el vendedor fijó al publicarla (nunca la
     * cotización vigente del jugador, que esta vía no consulta): valida cantidad disponible y saldo
     * del comprador a ese precio, y ejecuta atómicamente los 4 movimientos (débito de tokens +
     * crédito de saldo al vendedor, débito de saldo + crédito de tokens al comprador). Lee la
     * oferta bajo {@code @Lock(PESSIMISTIC_WRITE)} (plan.md §9.3): si dos compradores compiten por
     * la misma oferta, el segundo espera a que termine la transacción del primero.
     *
     * @return el {@link Movimiento} {@code COMPRA_P2P} del comprador
     * @throws RecursoNoEncontradoException si no existe una oferta con {@code ofertaId}
     * @throws OfertaInsuficienteException si {@code cantidad} supera {@code cantidadDisponible} de
     *     la oferta (cubre también una oferta ya {@code AGOTADA}/{@code CANCELADA}, que siempre
     *     tiene {@code cantidadDisponible = 0})
     * @throws SaldoInsuficienteException si el comprador no tiene saldo suficiente al precio
     *     publicado
     */
    Movimiento ejecutar(UUID compradorId, UUID ofertaId, int cantidad);

    /**
     * Cancela lo no vendido de una oferta propia (UC-17, plan.md §9.4): libera la reserva
     * correspondiente en la {@code TenenciaToken} del vendedor, sin registrar ningún {@link
     * Movimiento} (no hubo venta sobre esa cantidad).
     *
     * @throws RecursoNoEncontradoException si no existe una oferta con {@code ofertaId}
     * @throws OfertaNoPerteneceAlVendedorException si {@code vendedorId} no es el dueño de la
     *     oferta
     * @throws OfertaNoAbiertaException si la oferta ya está {@code AGOTADA} o {@code CANCELADA}
     */
    void cancelar(UUID vendedorId, UUID ofertaId);

    /**
     * Cancela automáticamente toda oferta {@code ABIERTA} cuya {@code fechaVencimiento} ya pasó
     * (UC-17: "si pasa esa cantidad de horas sin venderse, se cancela la oferta p2p y los tokens
     * vuelven al usuario que la creó"), liberando la reserva de cada una a la tenencia de su
     * vendedor — mismo mecanismo que {@link #cancelar}, sin {@link Movimiento}. Invocado
     * periódicamente por un chequeo programado en {@code config/} (nunca por HTTP, el trigger no es
     * un endpoint), nunca por el propio vendedor ni por un comprador. Una falla puntual al cancelar
     * una oferta vencida no debe impedir cancelar las demás de esa misma pasada — ver {@link
     * #cancelarUnaVencida}.
     */
    void cancelarVencidas();

    /**
     * Cancela, en su propia sub-transacción aislada, una única oferta vencida identificada por
     * {@code ofertaId} si sigue {@code ABIERTA} al momento de ejecutarse (puede haberse vendido o
     * cancelado voluntariamente entre que {@link #cancelarVencidas} la encontró y este momento).
     * Colaborador interno de {@link #cancelarVencidas} — expuesto en la interfaz únicamente para
     * que la auto-invocación pase por el proxy AOP de Spring (mismo criterio que {@code
     * CotizacionService#calcularCotizacion}, tasks.md T4.5), no pensado para invocarse por su
     * cuenta desde otro punto del sistema.
     */
    void cancelarUnaVencida(UUID ofertaId);
}
