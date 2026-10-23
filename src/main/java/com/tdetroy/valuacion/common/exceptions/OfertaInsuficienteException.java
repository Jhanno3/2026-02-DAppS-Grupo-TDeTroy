package com.tdetroy.valuacion.common.exceptions;

/**
 * La {@code cantidadDisponible} de una {@code OfertaP2P} no alcanza para la cantidad solicitada por
 * un comprador.
 *
 * <p>Lanzada por {@code OfertaServiceImpl.ejecutar} (plan.md §9.2, UC-17) <b>antes</b> de invocar
 * {@code OfertaP2P.ejecutarContra} — esa validación (paso 1 de §9.2: "cantidad ≤ cantidadDisponible
 * de la oferta") vive acá, como regla de negocio con status HTTP propio (409), nunca como el {@link
 * IllegalArgumentException} defensivo que {@code ejecutarContra} lanza si de todos modos se lo
 * invocara sin esta validación previa (constitution.md §2: la invariante vive en la entidad, pero
 * el error de cara al cliente lo decide el Service).
 */
public final class OfertaInsuficienteException extends NegocioException {

    public OfertaInsuficienteException(int cantidadSolicitada, int cantidadDisponible) {
        super(
                "Oferta insuficiente: se solicitaron %d tokens pero hay %d disponibles"
                        .formatted(cantidadSolicitada, cantidadDisponible));
    }
}
