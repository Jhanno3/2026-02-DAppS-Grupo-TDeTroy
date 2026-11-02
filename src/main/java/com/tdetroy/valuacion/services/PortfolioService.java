package com.tdetroy.valuacion.services;

import com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException;
import com.tdetroy.valuacion.model.Portfolio;
import java.util.UUID;

/**
 * Vista de lectura del portfolio de un usuario (plan.md §1/§2.5, UC-11), combinando {@code
 * TenenciaTokenRepository}, {@code CotizacionHistoricaRepository} y {@code SubastaRepository} — sin
 * entidad {@code Portfolio} persistida. La valorización (valores, precio promedio, ganancia/pérdida
 * y totales) la calcula {@link Portfolio#valorizar}; este Service sólo junta los datos.
 */
public interface PortfolioService {

    /**
     * Portfolio de {@code usuarioId}: sus tenencias con tokens disponibles ({@code cantidad -
     * cantidadReservada > 0}) — lo reservado en ofertas P2P abiertas o subastas activas va en
     * {@code valorEnVenta} —, cada una valorizada a la cotización vigente del jugador
     * correspondiente (o {@code app.cotizacion.valor-inicial} si ese jugador todavía no tiene
     * ningún cálculo persistido), más el saldo virtual y el valor total (spec.md UC-11). Un usuario
     * sin tenencias ve una lista vacía, nunca un error.
     *
     * @throws RecursoNoEncontradoException si no existe {@code usuarioId}
     */
    Portfolio obtenerDe(UUID usuarioId);
}
