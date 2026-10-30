package com.tdetroy.valuacion.services;

import com.tdetroy.valuacion.model.PujaSubasta;
import com.tdetroy.valuacion.model.Subasta;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Subasta de tokens entre usuarios (UC-18). Único punto que mueve tokens y saldo por una subasta:
 * reserva los tokens del vendedor al crearla, reserva (descuenta) el saldo del mejor postor en cada
 * puja y se lo devuelve al superado, y al cerrar adjudica, declara desierta o cancela — siempre en
 * una única transacción por operación (constitution.md §2).
 */
public interface SubastaService {

    /**
     * Crea una subasta activa y reserva {@code cantidad} tokens de la tenencia del vendedor.
     *
     * @throws com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException si el jugador no
     *     existe
     * @throws com.tdetroy.valuacion.common.exceptions.OperacionSubastaInvalidaException si el
     *     jugador no está activo
     * @throws com.tdetroy.valuacion.common.exceptions.TenenciaInsuficienteException si el vendedor
     *     no tiene esa cantidad disponible
     */
    Subasta crear(
            UUID vendedorId,
            UUID jugadorId,
            int cantidad,
            BigDecimal precioInicial,
            long duracionMinutos);

    /** Subastas activas, las más nuevas primero. */
    List<Subasta> listarActivas();

    /** Subastas activas de {@code jugadorId}, las más nuevas primero. */
    List<Subasta> listarActivasDe(UUID jugadorId);

    /**
     * Subasta (en cualquier estado) con su historial de pujas, la más reciente primero.
     *
     * @throws com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException si no existe
     */
    SubastaDetalle obtenerDetalle(UUID subastaId);

    /**
     * Registra una puja de {@code postorId} subiendo {@code porcentaje} (0, 5, 10 o 20) sobre el
     * precio vigente: le descuenta el monto total al postor y le devuelve el suyo al mejor postor
     * anterior, en la misma transacción.
     *
     * @throws com.tdetroy.valuacion.common.exceptions.OperacionSubastaInvalidaException si la puja
     *     no es válida (ver {@link Subasta#pujar})
     * @throws com.tdetroy.valuacion.common.exceptions.SaldoInsuficienteException si al postor no le
     *     alcanza el saldo
     */
    Subasta pujar(UUID postorId, UUID subastaId, int porcentaje);

    /**
     * El vendedor cancela su subasta activa: los tokens vuelven a su tenencia y, si había un mejor
     * postor, se le devuelve su saldo.
     *
     * @throws com.tdetroy.valuacion.common.exceptions.OperacionSubastaInvalidaException si la
     *     subasta no es de {@code vendedorId} o ya no está activa
     */
    void cancelar(UUID vendedorId, UUID subastaId);

    /**
     * Cancela todas las subastas activas de {@code jugadorId} (baja del jugador, UC-15), dentro de
     * la transacción de quien llama — antes de compensar las tenencias.
     */
    void cancelarActivasDe(UUID jugadorId);

    /**
     * Adjudica las subastas activas cuyo plazo de 2 minutos sin pujas ya pasó y declara desiertas
     * las que vencieron sin pujas. Una falla puntual no impide cerrar las demás. Pensada para un
     * scheduler en {@code config/}, nunca para un Controller.
     */
    void cerrarVencidas();

    /**
     * Cierra una subasta puntual si corresponde, en su propia transacción (ver {@link
     * #cerrarVencidas}).
     */
    void cerrarUna(UUID subastaId);

    /** Subasta junto a su historial de pujas, la más reciente primero. */
    record SubastaDetalle(Subasta subasta, List<PujaSubasta> pujas) {}
}
