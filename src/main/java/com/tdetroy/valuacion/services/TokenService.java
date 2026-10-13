package com.tdetroy.valuacion.services;

import com.tdetroy.valuacion.common.exceptions.EmisionMaximaSuperadaException;
import com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException;
import com.tdetroy.valuacion.common.exceptions.SaldoInsuficienteException;
import com.tdetroy.valuacion.common.exceptions.TenenciaInsuficienteException;
import com.tdetroy.valuacion.model.Movimiento;
import java.util.UUID;

/**
 * Compra y venta de tokens de un jugador contra el sistema (plan.md §1/§5, UC-09/UC-10).
 *
 * <p>Nunca calcula ni persiste un valor de cotización nuevo — único punto de verdad de {@code
 * CotizacionService} (constitution.md §2) — sólo lee la cotización vigente de un jugador: el último
 * {@code CotizacionHistorica} por {@code jugadorId} vía {@code CotizacionHistoricaRepository}, o
 * {@code app.cotizacion.valor-inicial} si todavía no tiene ningún cálculo persistido (UC-03: un
 * jugador recién dado de alta ya habilita compra "aún sin cotización calculada").
 *
 * <p>No existe tope de concentración: un usuario puede acumular tantos tokens de un mismo jugador
 * como su saldo le permita, sin más límite que el máximo de 100 tokens emitidos por jugador
 * (spec.md UC-09, regla 5 de criterios de aceptación) — este Service no agrega ninguna validación
 * de cantidad poseída por usuario.
 *
 * <p>No valida el {@code estado} del jugador: bloquear operaciones sobre un jugador ya {@code
 * INACTIVO} es responsabilidad de quien orquesta la baja completa (tasks.md T6.6, ver javadoc de
 * {@code Jugador#darDeBaja}), fuera de alcance de este Service en su forma actual.
 */
public interface TokenService {

    /**
     * Compra {@code cantidad} tokens de {@code jugadorId} a la cotización vigente, debitando el
     * costo total del saldo virtual de {@code usuarioId}, emitiendo esos tokens en el jugador y
     * acreditándolos en la tenencia de {@code usuarioId}; registra el {@link Movimiento}
     * correspondiente, tipo {@code COMPRA_SISTEMA} (UC-09). Todo o nada: cualquier rechazo deja
     * saldo, emisión y tenencia sin modificar.
     *
     * @throws RecursoNoEncontradoException si no existe {@code usuarioId} o {@code jugadorId}
     * @throws SaldoInsuficienteException si el saldo virtual de {@code usuarioId} no alcanza para
     *     el costo total a la cotización vigente
     * @throws EmisionMaximaSuperadaException si {@code cantidad} supera los tokens disponibles del
     *     jugador sobre el máximo de 100 emitidos — nunca se ejecuta una compra parcial
     * @throws IllegalArgumentException si {@code cantidad} no es mayor a cero
     */
    Movimiento comprar(UUID usuarioId, UUID jugadorId, int cantidad);

    /**
     * Vende {@code cantidad} tokens de {@code jugadorId} que posee {@code usuarioId} de vuelta al
     * sistema, a la cotización vigente: valida la cantidad <b>disponible</b> de la tenencia (vía
     * {@code TenenciaToken.disponible()} — nunca se puede vender una cantidad reservada en una
     * oferta P2P abierta, ver UC-17), descuenta la tenencia, acredita el saldo virtual
     * correspondiente, libera esa cantidad de vuelta a la emisión disponible del jugador y registra
     * el {@link Movimiento} correspondiente, tipo {@code VENTA_SISTEMA} (UC-10). Todo o nada.
     *
     * @throws RecursoNoEncontradoException si no existe {@code usuarioId} o {@code jugadorId}
     * @throws TenenciaInsuficienteException si {@code usuarioId} no tiene al menos {@code cantidad}
     *     tokens disponibles (sin reservar) de {@code jugadorId}
     * @throws IllegalArgumentException si {@code cantidad} no es mayor a cero
     */
    Movimiento vender(UUID usuarioId, UUID jugadorId, int cantidad);
}
