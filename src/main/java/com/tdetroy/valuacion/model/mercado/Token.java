package com.tdetroy.valuacion.model.mercado;

import com.tdetroy.valuacion.common.exceptions.EmisionMaximaSuperadaException;
import com.tdetroy.valuacion.model.jugador.Jugador;

/**
 * Emisión de tokens de un {@link Jugador}, con máximo de 100. No se persiste por su cuenta; los
 * Services deben usarla a través de {@link Jugador}.
 */
public final class Token {

    private static final int MAXIMO_EMITIDOS = 100;

    private int cantidadEmitida;

    private Token(int cantidadEmitida) {
        this.cantidadEmitida = cantidadEmitida;
    }

    public static Token nueva() {
        return new Token(0);
    }

    /** Reconstruye desde persistencia, sin validar. */
    public static Token reconstruir(int cantidadEmitida) {
        return new Token(cantidadEmitida);
    }

    /**
     * @throws IllegalArgumentException si {@code cantidad} no es mayor a cero
     * @throws EmisionMaximaSuperadaException si se superan los 100 tokens emitidos
     */
    public void emitir(int cantidad) {
        exigirPositivo(cantidad);
        if (cantidadEmitida + cantidad > MAXIMO_EMITIDOS) {
            throw new EmisionMaximaSuperadaException(cantidadEmitida, cantidad, MAXIMO_EMITIDOS);
        }
        this.cantidadEmitida += cantidad;
    }

    /**
     * @throws IllegalArgumentException si {@code cantidad} no es mayor a cero, o si supera la
     *     emitida
     */
    public void liberar(int cantidad) {
        exigirPositivo(cantidad);
        if (cantidad > cantidadEmitida) {
            throw new IllegalArgumentException(
                    "cantidad a liberar (%d) no puede superar la cantidad emitida actual (%d)"
                            .formatted(cantidad, cantidadEmitida));
        }
        this.cantidadEmitida -= cantidad;
    }

    public int disponibles() {
        return MAXIMO_EMITIDOS - cantidadEmitida;
    }

    public int getCantidadEmitida() {
        return cantidadEmitida;
    }

    private static void exigirPositivo(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("cantidad debe ser mayor a cero, fue " + cantidad);
        }
    }
}
