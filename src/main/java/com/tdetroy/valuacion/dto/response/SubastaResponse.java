package com.tdetroy.valuacion.dto.response;

import com.tdetroy.valuacion.model.EstadoSubasta;
import com.tdetroy.valuacion.model.IncrementoPuja;
import com.tdetroy.valuacion.model.PujaSubasta;
import com.tdetroy.valuacion.model.Subasta;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Subasta (UC-18), para el listado y el detalle. {@code cierraEn} es cuándo deja de aceptar pujas
 * (plazo de 2 minutos si ya hubo pujas, fin de la duración si no). {@code opcionesPuja} trae, ya
 * calculados por el backend, los precios exactos de cada incremento admitido en este momento —
 * vacío si la subasta no está activa. {@code pujas} sólo viene en el detalle ({@code null} en el
 * listado).
 */
public record SubastaResponse(
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
        Instant cierraEn,
        List<OpcionPuja> opcionesPuja,
        List<Puja> pujas) {

    public static SubastaResponse desde(Subasta subasta) {
        return desde(subasta, null);
    }

    public static SubastaResponse desde(Subasta subasta, List<PujaSubasta> pujas) {
        return new SubastaResponse(
                subasta.getId(),
                subasta.getVendedorId(),
                subasta.getJugadorId(),
                subasta.getCantidad(),
                subasta.getPrecioInicial(),
                subasta.getPrecioActual(),
                subasta.getMejorPostorId(),
                subasta.getCantidadPujas(),
                subasta.getEstado(),
                subasta.getFechaCreacion(),
                subasta.getFechaVencimiento(),
                subasta.cierraEn(),
                opcionesDe(subasta),
                pujas == null ? null : pujas.stream().map(Puja::desde).toList());
    }

    private static List<OpcionPuja> opcionesDe(Subasta subasta) {
        if (subasta.getEstado() != EstadoSubasta.ACTIVA) {
            return List.of();
        }
        return Arrays.stream(IncrementoPuja.values())
                .filter(
                        incremento ->
                                incremento != IncrementoPuja.PRECIO_INICIAL
                                        || !subasta.tienePujas())
                .map(
                        incremento -> {
                            BigDecimal precio = subasta.precioConIncremento(incremento);
                            return new OpcionPuja(
                                    incremento.getPorcentaje(), precio, subasta.montoTotal(precio));
                        })
                .toList();
    }

    /** Una opción de puja: incremento, precio por token resultante y monto total a reservar. */
    public record OpcionPuja(int porcentaje, BigDecimal precioUnitario, BigDecimal montoTotal) {}

    /** Una puja del historial. */
    public record Puja(UUID postorId, BigDecimal precioUnitario, Instant fecha) {

        static Puja desde(PujaSubasta puja) {
            return new Puja(puja.postorId(), puja.precioUnitario(), puja.fecha());
        }
    }
}
