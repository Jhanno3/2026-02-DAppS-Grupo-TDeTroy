package com.tdetroy.valuacion.model;

import com.tdetroy.valuacion.common.Monetario;
import com.tdetroy.valuacion.common.exceptions.TenenciaInsuficienteException;
import java.math.BigDecimal;
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
 *
 * <p><b>Costo de adquisición (costo promedio ponderado):</b> {@code costoTotal} es lo que el
 * usuario pagó por los {@code cantidad} tokens que tiene hoy. Cada compra suma {@code cantidad ×
 * precio pagado}; cada salida de tokens ({@link #debitar}, {@link #ejecutarReserva}) descuenta la
 * parte proporcional, así que vender nunca cambia el {@link #precioPromedioCompra()} de lo que
 * queda. Reservar/liberar no lo tocan: los tokens reservados siguen siendo del usuario.
 *
 * <p><b>Ganancia realizada:</b> {@code gananciaRealizada} acumula, por cada salida de tokens, lo
 * cobrado menos el costo de lo que salió. Sobrevive a la tenencia: sigue contando aunque {@code
 * cantidad} llegue a cero. Ver {@link #resultadoTotal}.
 */
@Getter
public class TenenciaToken {

    private final UUID id;
    private final UUID usuarioId;
    private final UUID jugadorId;
    private int cantidad;
    private int cantidadReservada;
    private BigDecimal costoTotal;
    private BigDecimal gananciaRealizada;

    private TenenciaToken(UUID usuarioId, UUID jugadorId) {
        validarCampos(usuarioId, jugadorId);

        this.id = UUID.randomUUID();
        this.usuarioId = usuarioId;
        this.jugadorId = jugadorId;
        this.cantidad = 0;
        this.cantidadReservada = 0;
        this.costoTotal = Monetario.CERO;
        this.gananciaRealizada = Monetario.CERO;
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
            UUID id,
            UUID usuarioId,
            UUID jugadorId,
            int cantidad,
            int cantidadReservada,
            BigDecimal costoTotal,
            BigDecimal gananciaRealizada) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.jugadorId = jugadorId;
        this.cantidad = cantidad;
        this.cantidadReservada = cantidadReservada;
        this.costoTotal = costoTotal;
        this.gananciaRealizada = gananciaRealizada;
    }

    /**
     * Reconstruye una {@code TenenciaToken} ya persistida a partir de sus datos crudos (usado por
     * {@code entity/TenenciaTokenEntity#aModelo()} en el límite con {@code repositories/}) — a
     * diferencia de {@link #abrir}, no valida las invariantes de alta.
     */
    public static TenenciaToken reconstruir(
            UUID id,
            UUID usuarioId,
            UUID jugadorId,
            int cantidad,
            int cantidadReservada,
            BigDecimal costoTotal,
            BigDecimal gananciaRealizada) {
        return new TenenciaToken(
                id,
                usuarioId,
                jugadorId,
                cantidad,
                cantidadReservada,
                costoTotal,
                gananciaRealizada);
    }

    /** Cantidad disponible para venta al sistema o para publicar en una nueva oferta P2P. */
    public int disponible() {
        return cantidad - cantidadReservada;
    }

    /**
     * Acredita {@code cantidad} tokens comprados a {@code precioUnitario} (compra al sistema,
     * compra P2P o adjudicación de subasta): suma {@code cantidad × precioUnitario} al costo de
     * adquisición.
     *
     * @throws IllegalArgumentException si {@code cantidad} no es mayor a cero, o si {@code
     *     precioUnitario} no es mayor a cero
     */
    public void acreditar(int cantidad, BigDecimal precioUnitario) {
        requirePositivo(cantidad, "cantidad");
        Objects.requireNonNull(precioUnitario, "precioUnitario no puede ser null");
        if (precioUnitario.signum() <= 0) {
            throw new IllegalArgumentException(
                    "precioUnitario debe ser mayor a cero, fue " + precioUnitario);
        }
        this.cantidad += cantidad;
        this.costoTotal =
                Monetario.escalar(
                        costoTotal.add(precioUnitario.multiply(BigDecimal.valueOf(cantidad))));
    }

    /**
     * Debita {@code cantidad} de la tenencia (venta al sistema, UC-10; retiro por baja de jugador),
     * descontando su parte proporcional del costo de adquisición y realizando su resultado.
     *
     * @param montoCobrado lo que recibe el usuario por estos tokens
     * @throws IllegalArgumentException si {@code cantidad} no es mayor a cero, o si {@code
     *     montoCobrado} es negativo
     * @throws TenenciaInsuficienteException si {@code cantidad} supera {@link #disponible()}
     */
    public void debitar(int cantidad, BigDecimal montoCobrado) {
        validarDebito(cantidad);
        retirar(cantidad, montoCobrado);
    }

    /**
     * Valida que se puedan debitar {@code cantidad} tokens sin mutar nada, para cortar antes de
     * calcular el monto a cobrar.
     *
     * @throws IllegalArgumentException si {@code cantidad} no es mayor a cero
     * @throws TenenciaInsuficienteException si {@code cantidad} supera {@link #disponible()}
     */
    public void validarDebito(int cantidad) {
        requirePositivo(cantidad, "cantidad");
        if (cantidad > disponible()) {
            throw new TenenciaInsuficienteException(cantidad, disponible());
        }
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
     * concretada contra la oferta del vendedor, UC-17, o adjudicación de subasta, UC-18) —
     * descuenta {@code cantidad} y {@code cantidadReservada} a la vez, y la parte proporcional del
     * costo de adquisición, realizando su resultado.
     *
     * @param montoCobrado lo que recibe el vendedor por estos tokens
     * @throws IllegalArgumentException si {@code cantidad} no es mayor a cero, si supera {@code
     *     cantidadReservada}, o si {@code montoCobrado} es negativo
     */
    public void ejecutarReserva(int cantidad, BigDecimal montoCobrado) {
        requirePositivo(cantidad, "cantidad");
        if (cantidad > cantidadReservada) {
            throw new IllegalArgumentException(
                    "cantidad a ejecutar (%d) no puede superar cantidadReservada actual (%d)"
                            .formatted(cantidad, cantidadReservada));
        }
        retirar(cantidad, montoCobrado);
        this.cantidadReservada -= cantidad;
    }

    /**
     * Precio promedio pagado por token ({@code costoTotal / cantidad}); cero si la tenencia está
     * vacía.
     */
    public BigDecimal precioPromedioCompra() {
        if (cantidad == 0) {
            return Monetario.CERO;
        }
        return costoTotal.divide(
                BigDecimal.valueOf(cantidad), Monetario.ESCALA, Monetario.REDONDEO);
    }

    /** Valor de mercado de los tokens disponibles a {@code cotizacion}. */
    public BigDecimal valorDisponible(BigDecimal cotizacion) {
        return valorizar(cotizacion, disponible());
    }

    /** Valor de mercado de los tokens reservados (ofertas P2P / subastas) a {@code cotizacion}. */
    public BigDecimal valorReservado(BigDecimal cotizacion) {
        return valorizar(cotizacion, cantidadReservada);
    }

    /**
     * Ganancia (positiva) o pérdida (negativa) no realizada de los tokens disponibles: valor a
     * {@code cotizacion} menos lo que costaron. Ej.: 10 tokens comprados a 100 con cotización 120 →
     * {@code 1200 - 1000 = +200}.
     */
    public BigDecimal gananciaPerdida(BigDecimal cotizacion) {
        return valorDisponible(cotizacion).subtract(costoDisponible());
    }

    /**
     * Resultado total de la tenencia: lo ya realizado en ventas más lo no realizado de todos los
     * tokens que quedan, reservados incluidos. Ej.: 10 tokens comprados a 100, 5 vendidos a 150 y
     * cotización 120 → {@code 250 + (600 - 500) = +350}.
     */
    public BigDecimal resultadoTotal(BigDecimal cotizacion) {
        return gananciaRealizada.add(valorizar(cotizacion, cantidad)).subtract(costoTotal);
    }

    /**
     * Rentabilidad porcentual de los tokens disponibles ({@code gananciaPerdida / costo × 100});
     * cero si no tienen costo registrado.
     */
    public BigDecimal rentabilidad(BigDecimal cotizacion) {
        BigDecimal costo = costoDisponible();
        if (costo.signum() == 0) {
            return Monetario.CERO;
        }
        return gananciaPerdida(cotizacion)
                .multiply(BigDecimal.valueOf(100))
                .divide(costo, Monetario.ESCALA, Monetario.REDONDEO);
    }

    /**
     * Costo de adquisición de los tokens disponibles: la parte proporcional de {@code costoTotal}
     * (no {@code precioPromedio × disponibles}, que arrastraría el redondeo del promedio).
     */
    private BigDecimal costoDisponible() {
        return costoProporcional(disponible());
    }

    /**
     * Saca {@code cantidad} tokens descontando su costo proporcional y sumando a {@code
     * gananciaRealizada} lo cobrado menos ese costo; si la tenencia queda vacía, el costo vuelve a
     * cero exacto (sin residuos de redondeo).
     */
    private void retirar(int cantidadRetirada, BigDecimal montoCobrado) {
        Objects.requireNonNull(montoCobrado, "montoCobrado no puede ser null");
        if (montoCobrado.signum() < 0) {
            throw new IllegalArgumentException(
                    "montoCobrado no puede ser negativo, fue " + montoCobrado);
        }
        BigDecimal costoRestante = costoProporcional(cantidad - cantidadRetirada);
        BigDecimal costoRetirado = costoTotal.subtract(costoRestante);
        this.gananciaRealizada =
                Monetario.escalar(gananciaRealizada.add(montoCobrado).subtract(costoRetirado));
        this.costoTotal = costoRestante;
        this.cantidad -= cantidadRetirada;
    }

    /** {@code costoTotal × parte / cantidad}, redondeado a escala monetaria. */
    private BigDecimal costoProporcional(int parte) {
        if (parte == cantidad) {
            return costoTotal;
        }
        if (parte == 0) {
            return Monetario.CERO;
        }
        return costoTotal
                .multiply(BigDecimal.valueOf(parte))
                .divide(BigDecimal.valueOf(cantidad), Monetario.ESCALA, Monetario.REDONDEO);
    }

    private static BigDecimal valorizar(BigDecimal cotizacion, int cantidad) {
        Objects.requireNonNull(cotizacion, "cotizacion no puede ser null");
        return Monetario.escalar(cotizacion.multiply(BigDecimal.valueOf(cantidad)));
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
