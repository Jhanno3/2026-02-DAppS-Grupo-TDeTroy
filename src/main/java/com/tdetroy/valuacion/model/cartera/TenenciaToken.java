package com.tdetroy.valuacion.model.cartera;

import com.tdetroy.valuacion.common.Monetario;
import com.tdetroy.valuacion.common.exceptions.TenenciaInsuficienteException;
import com.tdetroy.valuacion.model.jugador.Jugador;
import com.tdetroy.valuacion.model.usuario.Usuario;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * Tokens de un {@link Jugador} en poder de un {@link Usuario}. Invariante: {@code cantidadReservada
 * ≤ cantidad}.
 *
 * <p>{@code costoTotal} usa costo promedio ponderado: las compras suman lo pagado y las salidas
 * descuentan su parte proporcional, así que vender no cambia {@link #precioPromedioCompra()}.
 * Reservar o liberar no lo tocan.
 *
 * <p>{@code gananciaRealizada} acumula lo cobrado menos el costo de cada salida, y se conserva
 * aunque {@code cantidad} llegue a cero.
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

    /** Reconstruye desde persistencia; no valida las invariantes de alta. */
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

    public int disponible() {
        return cantidad - cantidadReservada;
    }

    /**
     * Acredita tokens comprados, sumando lo pagado al costo de adquisición.
     *
     * @throws IllegalArgumentException si {@code cantidad} o {@code precioUnitario} no son mayores
     *     a cero
     */
    public void acreditar(int cantidad, BigDecimal precioUnitario) {
        exigirPositivo(cantidad, "cantidad");
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
     * Debita tokens disponibles, descontando su costo proporcional y realizando su resultado.
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
     * Valida un débito sin mutar nada, para cortar antes de calcular el monto a cobrar.
     *
     * @throws IllegalArgumentException si {@code cantidad} no es mayor a cero
     * @throws TenenciaInsuficienteException si {@code cantidad} supera {@link #disponible()}
     */
    public void validarDebito(int cantidad) {
        exigirPositivo(cantidad, "cantidad");
        if (cantidad > disponible()) {
            throw new TenenciaInsuficienteException(cantidad, disponible());
        }
    }

    /**
     * Reserva tokens: dejan de estar disponibles hasta que se libere la reserva.
     *
     * @throws IllegalArgumentException si {@code cantidad} no es mayor a cero
     * @throws TenenciaInsuficienteException si {@code cantidad} supera {@link #disponible()}
     */
    public void reservar(int cantidad) {
        exigirPositivo(cantidad, "cantidad");
        if (cantidad > disponible()) {
            throw new TenenciaInsuficienteException(cantidad, disponible());
        }
        this.cantidadReservada += cantidad;
    }

    /**
     * Libera reserva sin vender nada.
     *
     * @throws IllegalArgumentException si {@code cantidad} no es mayor a cero, o si supera {@code
     *     cantidadReservada}
     */
    public void liberarReserva(int cantidad) {
        exigirPositivo(cantidad, "cantidad");
        if (cantidad > cantidadReservada) {
            throw new IllegalArgumentException(
                    "cantidad a liberar (%d) no puede superar cantidadReservada actual (%d)"
                            .formatted(cantidad, cantidadReservada));
        }
        this.cantidadReservada -= cantidad;
    }

    /**
     * Concreta la venta de tokens reservados: salen de la tenencia junto con su costo proporcional,
     * realizando su resultado.
     *
     * @param montoCobrado lo que recibe el vendedor por estos tokens
     * @throws IllegalArgumentException si {@code cantidad} no es mayor a cero, si supera {@code
     *     cantidadReservada}, o si {@code montoCobrado} es negativo
     */
    public void ejecutarReserva(int cantidad, BigDecimal montoCobrado) {
        exigirPositivo(cantidad, "cantidad");
        if (cantidad > cantidadReservada) {
            throw new IllegalArgumentException(
                    "cantidad a ejecutar (%d) no puede superar cantidadReservada actual (%d)"
                            .formatted(cantidad, cantidadReservada));
        }
        retirar(cantidad, montoCobrado);
        this.cantidadReservada -= cantidad;
    }

    public BigDecimal precioPromedioCompra() {
        if (cantidad == 0) {
            return Monetario.CERO;
        }
        return costoTotal.divide(
                BigDecimal.valueOf(cantidad), Monetario.ESCALA, Monetario.REDONDEO);
    }

    public BigDecimal valorDisponible(BigDecimal cotizacion) {
        return valorizar(cotizacion, disponible());
    }

    public BigDecimal valorReservado(BigDecimal cotizacion) {
        return valorizar(cotizacion, cantidadReservada);
    }

    /** Resultado no realizado de los tokens disponibles. */
    public BigDecimal gananciaPerdida(BigDecimal cotizacion) {
        return valorDisponible(cotizacion).subtract(costoDisponible());
    }

    /** Resultado realizado más no realizado de todos los tokens, reservados incluidos. */
    public BigDecimal resultadoTotal(BigDecimal cotizacion) {
        return gananciaRealizada.add(valorizar(cotizacion, cantidad)).subtract(costoTotal);
    }

    /** Rentabilidad porcentual de los tokens disponibles; cero si no tienen costo. */
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
     * Parte proporcional de {@code costoTotal}; usar el precio promedio arrastraría su redondeo.
     */
    private BigDecimal costoDisponible() {
        return costoProporcional(disponible());
    }

    /** Si la tenencia queda vacía, el costo vuelve a cero exacto, sin residuos de redondeo. */
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

    private static void exigirPositivo(int valor, String nombreCampo) {
        if (valor <= 0) {
            throw new IllegalArgumentException(
                    nombreCampo + " debe ser mayor a cero, fue " + valor);
        }
    }
}
