package com.tdetroy.valuacion.model.mercado;

import com.tdetroy.valuacion.common.exceptions.OperacionSubastaInvalidaException;
import java.util.Arrays;

/**
 * Incrementos admitidos, nunca un monto libre. {@link #PRECIO_INICIAL} sólo vale como primera puja.
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
