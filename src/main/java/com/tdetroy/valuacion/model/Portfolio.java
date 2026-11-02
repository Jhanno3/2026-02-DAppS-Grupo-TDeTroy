package com.tdetroy.valuacion.model;

import com.tdetroy.valuacion.common.Monetario;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Portfolio de un usuario valorizado a la cotización vigente (UC-11) — vista de sólo lectura
 * calculada a partir de sus {@link TenenciaToken}, no se persiste (plan.md §2.5: "no existe tabla
 * Portfolio separada").
 *
 * @param tenencias posiciones con tokens disponibles (los reservados van en {@code valorEnVenta})
 * @param valorEnVenta tokens reservados en ofertas P2P abiertas y subastas activas, a cotización
 *     vigente: no están disponibles para operar, pero siguen siendo del usuario
 * @param saldoEnPujas saldo reservado como mejor postor de subastas activas (UC-18)
 * @param gananciaPerdida suma de la ganancia/pérdida no realizada de {@code tenencias}
 * @param valorTotal saldo + tenencias + valor en venta + saldo en pujas
 */
public record Portfolio(
        BigDecimal saldoVirtual,
        List<TenenciaPortfolio> tenencias,
        BigDecimal valorEnVenta,
        BigDecimal saldoEnPujas,
        BigDecimal gananciaPerdida,
        BigDecimal valorTotal) {

    /**
     * Posición en un jugador: tokens disponibles, lo que valen hoy y cuánto se gana o pierde
     * respecto de lo que costaron (ver {@link TenenciaToken}).
     *
     * @param rentabilidad porcentaje sobre el costo ({@code 20.00} = +20%)
     */
    public record TenenciaPortfolio(
            UUID jugadorId,
            int cantidad,
            BigDecimal cotizacionVigente,
            BigDecimal valor,
            BigDecimal precioPromedioCompra,
            BigDecimal gananciaPerdida,
            BigDecimal rentabilidad) {}

    /**
     * Valoriza las tenencias del usuario.
     *
     * @param cotizacionesVigentes cotización vigente de cada jugador presente en {@code tenencias}
     * @param subastasComoMejorPostor subastas activas donde el usuario es el mejor postor
     * @throws IllegalArgumentException si falta la cotización de algún jugador de {@code tenencias}
     */
    public static Portfolio valorizar(
            BigDecimal saldoVirtual,
            List<TenenciaToken> tenencias,
            Map<UUID, BigDecimal> cotizacionesVigentes,
            List<Subasta> subastasComoMejorPostor) {
        Objects.requireNonNull(saldoVirtual, "saldoVirtual no puede ser null");
        Objects.requireNonNull(tenencias, "tenencias no puede ser null");
        Objects.requireNonNull(cotizacionesVigentes, "cotizacionesVigentes no puede ser null");
        Objects.requireNonNull(
                subastasComoMejorPostor, "subastasComoMejorPostor no puede ser null");

        List<TenenciaPortfolio> posiciones =
                tenencias.stream()
                        .filter(tenencia -> tenencia.disponible() > 0)
                        .map(
                                tenencia ->
                                        posicionDe(
                                                tenencia,
                                                cotizacionDe(tenencia, cotizacionesVigentes)))
                        .toList();
        BigDecimal valorEnVenta =
                sumar(
                        tenencias.stream()
                                .map(
                                        tenencia ->
                                                tenencia.valorReservado(
                                                        cotizacionDe(
                                                                tenencia, cotizacionesVigentes)))
                                .toList());
        BigDecimal saldoEnPujas =
                sumar(subastasComoMejorPostor.stream().map(Subasta::montoComprometido).toList());
        BigDecimal valorPosiciones =
                sumar(posiciones.stream().map(TenenciaPortfolio::valor).toList());
        BigDecimal gananciaPerdida =
                sumar(posiciones.stream().map(TenenciaPortfolio::gananciaPerdida).toList());
        BigDecimal valorTotal =
                Monetario.escalar(
                        saldoVirtual.add(valorPosiciones).add(valorEnVenta).add(saldoEnPujas));

        return new Portfolio(
                saldoVirtual, posiciones, valorEnVenta, saldoEnPujas, gananciaPerdida, valorTotal);
    }

    private static TenenciaPortfolio posicionDe(TenenciaToken tenencia, BigDecimal cotizacion) {
        return new TenenciaPortfolio(
                tenencia.getJugadorId(),
                tenencia.disponible(),
                cotizacion,
                tenencia.valorDisponible(cotizacion),
                tenencia.precioPromedioCompra(),
                tenencia.gananciaPerdida(cotizacion),
                tenencia.rentabilidad(cotizacion));
    }

    private static BigDecimal cotizacionDe(
            TenenciaToken tenencia, Map<UUID, BigDecimal> cotizacionesVigentes) {
        BigDecimal cotizacion = cotizacionesVigentes.get(tenencia.getJugadorId());
        if (cotizacion == null) {
            throw new IllegalArgumentException(
                    "falta la cotización vigente del jugador " + tenencia.getJugadorId());
        }
        return cotizacion;
    }

    private static BigDecimal sumar(List<BigDecimal> valores) {
        return Monetario.escalar(valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
