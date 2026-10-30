package com.tdetroy.valuacion.common.exceptions;

/**
 * Se intentó una operación sobre una {@code Subasta} que sus reglas no admiten (UC-18): pujar en
 * una subasta que ya no está activa o ya cerró, que el vendedor puje en la propia, que el mejor
 * postor vuelva a pujar sobre sí mismo, un incremento fuera de 5/10/20%, o cancelar una subasta
 * ajena. El mensaje describe el caso puntual.
 */
public final class OperacionSubastaInvalidaException extends NegocioException {

    public OperacionSubastaInvalidaException(String message) {
        super(message);
    }
}
