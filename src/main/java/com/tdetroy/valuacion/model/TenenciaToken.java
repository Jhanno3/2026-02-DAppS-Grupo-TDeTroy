package com.tdetroy.valuacion.model;

import com.tdetroy.valuacion.common.exceptions.TenenciaInsuficienteException;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * Tenencia de tokens de un {@link Jugador} en poder de un {@link Usuario} (plan.md §2.5) — base de
 * lectura del portfolio; no existe una entidad {@code Portfolio} separada.
 *
 * <p>{@code cantidad}/{@code cantidadReservada} nunca se exponen con un setter público: sólo se
 * mueven a través de {@link #acreditar}, {@link #debitar}, {@link #reservar}, {@link
 * #liberarReserva} y {@link #ejecutarReserva}, que validan la invariante {@code cantidadReservada ≤
 * cantidad} en la propia entidad (constitution.md §2) — mismo criterio que la invariante de 100
 * tokens en {@link Jugador}, nunca sólo validada en el Service que las invoca.
 */
@Getter
public class TenenciaToken {

    private final UUID id;
    private final UUID usuarioId;
    private final UUID jugadorId;
    private int cantidad;
    private int cantidadReservada;

    private TenenciaToken(UUID usuarioId, UUID jugadorId) {
        validarCampos(usuarioId, jugadorId);

        this.id = UUID.randomUUID();
        this.usuarioId = usuarioId;
        this.jugadorId = jugadorId;
        this.cantidad = 0;
        this.cantidadReservada = 0;
    }

    /**
     * Abre una tenencia vacía para el par {@code usuarioId}/{@code jugadorId} — se acredita después
     * vía {@link #acreditar} (primera compra, UC-09). Único punto de creación de {@code
     * TenenciaToken}.
     */
    public static TenenciaToken abrir(UUID usuarioId, UUID jugadorId) {
        return new TenenciaToken(usuarioId, jugadorId);
    }

    private TenenciaToken(
            UUID id, UUID usuarioId, UUID jugadorId, int cantidad, int cantidadReservada) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.jugadorId = jugadorId;
        this.cantidad = cantidad;
        this.cantidadReservada = cantidadReservada;
    }

    /**
     * Reconstruye una {@code TenenciaToken} ya persistida a partir de sus datos crudos (usado por
     * {@code entity/TenenciaTokenEntity#aModelo()} en el límite con {@code repositories/}) — a
     * diferencia de {@link #abrir}, no valida las invariantes de alta.
     */
    public static TenenciaToken reconstruir(
            UUID id, UUID usuarioId, UUID jugadorId, int cantidad, int cantidadReservada) {
        return new TenenciaToken(id, usuarioId, jugadorId, cantidad, cantidadReservada);
    }

    /** Cantidad disponible para venta al sistema o para publicar en una nueva oferta P2P. */
    public int disponible() {
        return cantidad - cantidadReservada;
    }

    /**
     * Acredita {@code cantidad} a la tenencia (compra al sistema, compra P2P, reintegro por
     * cancelación de oferta o por baja de jugador).
     *
     * @throws IllegalArgumentException si {@code cantidad} no es mayor a cero
     */
    public void acreditar(int cantidad) {
        requirePositivo(cantidad, "cantidad");
        this.cantidad += cantidad;
    }

    /**
     * Debita {@code cantidad} de la tenencia (venta al sistema, UC-10).
     *
     * @throws IllegalArgumentException si {@code cantidad} no es mayor a cero
     * @throws TenenciaInsuficienteException si {@code cantidad} supera {@link #disponible()}
     */
    public void debitar(int cantidad) {
        requirePositivo(cantidad, "cantidad");
        if (cantidad > disponible()) {
            throw new TenenciaInsuficienteException(cantidad, disponible());
        }
        this.cantidad -= cantidad;
    }

    /**
     * Reserva {@code cantidad} al publicar una oferta P2P (UC-17); dejan de estar disponibles para
     * venta al sistema o para otra oferta hasta que se libere la reserva.
     *
     * @throws IllegalArgumentException si {@code cantidad} no es mayor a cero
     * @throws TenenciaInsuficienteException si {@code cantidad} supera {@link #disponible()}
     */
    public void reservar(int cantidad) {
        requirePositivo(cantidad, "cantidad");
        if (cantidad > disponible()) {
            throw new TenenciaInsuficienteException(cantidad, disponible());
        }
        this.cantidadReservada += cantidad;
    }

    /**
     * Libera {@code cantidad} de la reserva sin vender nada (cancelación total o parcial de una
     * oferta, o cancelación automática por baja del jugador).
     *
     * @throws IllegalArgumentException si {@code cantidad} no es mayor a cero, o si supera {@code
     *     cantidadReservada}
     */
    public void liberarReserva(int cantidad) {
        requirePositivo(cantidad, "cantidad");
        if (cantidad > cantidadReservada) {
            throw new IllegalArgumentException(
                    "cantidad a liberar (%d) no puede superar cantidadReservada actual (%d)"
                            .formatted(cantidad, cantidadReservada));
        }
        this.cantidadReservada -= cantidad;
    }

    /**
     * Ejecuta {@code cantidad} de la reserva: sale de la tenencia por completo (venta P2P
     * concretada contra la oferta del vendedor, UC-17) — descuenta {@code cantidad} y {@code
     * cantidadReservada} a la vez.
     *
     * @throws IllegalArgumentException si {@code cantidad} no es mayor a cero, o si supera {@code
     *     cantidadReservada}
     */
    public void ejecutarReserva(int cantidad) {
        requirePositivo(cantidad, "cantidad");
        if (cantidad > cantidadReservada) {
            throw new IllegalArgumentException(
                    "cantidad a ejecutar (%d) no puede superar cantidadReservada actual (%d)"
                            .formatted(cantidad, cantidadReservada));
        }
        this.cantidad -= cantidad;
        this.cantidadReservada -= cantidad;
    }

    private static void validarCampos(UUID usuarioId, UUID jugadorId) {
        Objects.requireNonNull(usuarioId, "usuarioId no puede ser null");
        Objects.requireNonNull(jugadorId, "jugadorId no puede ser null");
    }

    private static void requirePositivo(int valor, String nombreCampo) {
        if (valor <= 0) {
            throw new IllegalArgumentException(
                    nombreCampo + " debe ser mayor a cero, fue " + valor);
        }
    }
}
