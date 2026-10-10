package com.tdetroy.valuacion.model;

import com.tdetroy.valuacion.common.exceptions.EmisionMaximaSuperadaException;

/**
 * Emisión de tokens en circulación de un {@link Jugador} (plan.md §2.2, constitution.md §2:
 * invariante de máximo 100 tokens emitidos — la constitución permite protegerla "en la propia
 * entidad {@code model/Jugador} (o {@code Token}"; esta clase es esa segunda opción).
 *
 * <p>Objeto de valor compuesto por {@link Jugador}, nunca persistido por su cuenta: {@code
 * entity/JugadorEntity} sigue reflejando una única columna {@code tokens_emitidos} (sin tabla ni
 * migración nueva) — {@link Jugador} traduce hacia/desde esta clase en el mismo límite donde ya
 * traduce el resto de sus propios campos, y sigue siendo el único punto de entrada público para
 * emitir/liberar/consultar tokens (este tipo es package-private en su superficie mutable a
 * propósito, para que ningún Service la use salteando a {@link Jugador}).
 *
 * <p>Nunca expone un setter público sobre {@code cantidadEmitida}: sólo se mueve a través de {@link
 * #emitir}/{@link #liberar}, que validan acá la invariante de emisión máxima (constitution.md §2) —
 * nunca queda a criterio de {@link Jugador} ni del Service que lo invoca validar ese máximo por su
 * cuenta.
 */
public final class Token {

    private static final int MAXIMO_EMITIDOS = 100;

    private int cantidadEmitida;

    private Token(int cantidadEmitida) {
        this.cantidadEmitida = cantidadEmitida;
    }

    /** Emisión en cero, para un jugador recién dado de alta (UC-03). */
    static Token nueva() {
        return new Token(0);
    }

    /**
     * Reconstruye una emisión ya persistida (usada por {@link Jugador#reconstruir}) — a diferencia
     * de {@link #nueva()}, no valida cómo se llegó a ese valor, ya validado cuando se originó.
     */
    static Token reconstruir(int cantidadEmitida) {
        return new Token(cantidadEmitida);
    }

    /**
     * Emite {@code cantidad} tokens nuevos hacia circulación (compra al sistema, UC-09).
     *
     * @throws IllegalArgumentException si {@code cantidad} no es mayor a cero
     * @throws EmisionMaximaSuperadaException si {@code cantidadEmitida + cantidad} supera 100
     */
    void emitir(int cantidad) {
        requirePositivo(cantidad);
        if (cantidadEmitida + cantidad > MAXIMO_EMITIDOS) {
            throw new EmisionMaximaSuperadaException(cantidadEmitida, cantidad, MAXIMO_EMITIDOS);
        }
        this.cantidadEmitida += cantidad;
    }

    /**
     * Libera {@code cantidad} tokens de circulación, devolviéndolos a la emisión disponible (venta
     * al sistema, UC-10; constitution.md §2, regla 8 de spec.md §5).
     *
     * @throws IllegalArgumentException si {@code cantidad} no es mayor a cero, o si supera {@code
     *     cantidadEmitida}
     */
    void liberar(int cantidad) {
        requirePositivo(cantidad);
        if (cantidad > cantidadEmitida) {
            throw new IllegalArgumentException(
                    "cantidad a liberar (%d) no puede superar la cantidad emitida actual (%d)"
                            .formatted(cantidad, cantidadEmitida));
        }
        this.cantidadEmitida -= cantidad;
    }

    /** Tokens disponibles para una emisión nueva, sobre el máximo de 100. */
    int disponibles() {
        return MAXIMO_EMITIDOS - cantidadEmitida;
    }

    int getCantidadEmitida() {
        return cantidadEmitida;
    }

    private static void requirePositivo(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("cantidad debe ser mayor a cero, fue " + cantidad);
        }
    }
}
