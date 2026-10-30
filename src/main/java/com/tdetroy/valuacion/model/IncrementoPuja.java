package com.tdetroy.valuacion.model;

import com.tdetroy.valuacion.common.exceptions.OperacionSubastaInvalidaException;
import java.util.Arrays;

/**
 * Incrementos admitidos para una puja de {@link Subasta} (UC-18, constitution.md §4: nunca un monto
 * libre). {@link #PRECIO_INICIAL} sólo es válido como primera puja: iguala el precio inicial fijado
 * por el vendedor.
 */
public enum IncrementoPuja {
    PRECIO_INICIAL(0),
    CINCO(5),
    DIEZ(10),
    VEINTE(20);

    private final int porcentaje;

    IncrementoPuja(int porcentaje) {
        this.porcentaje = porcentaje;
    }

    public int getPorcentaje() {
        return porcentaje;
    }

    /**
     * @throws OperacionSubastaInvalidaException si {@code porcentaje} no es 0, 5, 10 ni 20
     */
    public static IncrementoPuja desdePorcentaje(int porcentaje) {
        return Arrays.stream(values())
                .filter(incremento -> incremento.porcentaje == porcentaje)
                .findFirst()
                .orElseThrow(
                        () ->
                                new OperacionSubastaInvalidaException(
                                        String.format(
                                                "El incremento de una puja tiene que ser 5, 10 o"
                                                        + " 20%% (o 0%% para igualar el precio"
                                                        + " inicial), fue %d%%",
                                                porcentaje)));
    }
}
