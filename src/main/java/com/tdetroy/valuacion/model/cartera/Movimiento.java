package com.tdetroy.valuacion.model.cartera;

import com.tdetroy.valuacion.common.Monetario;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * Movimiento de cara al usuario, append-only (el log interno es {@code RegistroAuditoria}). Campos
 * válidos según {@link #tipo}:
 *
 * <ul>
 *   <li>{@link TipoMovimiento#RECARGA_SALDO}: sin jugador, cantidad, precio ni contraparte.
 *   <li>Cualquier otro tipo: jugador, cantidad y precio obligatorios.
 *   <li>{@code contraparteUsuarioId}: obligatorio sólo en P2P y subasta, {@code null} en el resto.
 * </ul>
 */
@Getter
public class Movimiento {

    private final UUID id;
    private final UUID usuarioId;
    private final UUID jugadorId;
    private final TipoMovimiento tipo;
    private final Integer cantidad;
    private final BigDecimal precioUnitario;
    private final BigDecimal montoTotal;
    private final UUID contraparteUsuarioId;
    private final Instant fecha;

    private Movimiento(
            UUID usuarioId,
            UUID jugadorId,
            TipoMovimiento tipo,
            Integer cantidad,
            BigDecimal precioUnitario,
            BigDecimal montoTotal,
            UUID contraparteUsuarioId,
            Instant fecha) {
        validarCamposObligatorios(usuarioId, tipo, montoTotal, fecha);
        validarCamposJugador(tipo, jugadorId, cantidad, precioUnitario);
        validarContraparte(tipo, contraparteUsuarioId);

        this.id = UUID.randomUUID();
        this.usuarioId = usuarioId;
        this.jugadorId = jugadorId;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.montoTotal = montoTotal;
        this.contraparteUsuarioId = contraparteUsuarioId;
        this.fecha = fecha;
    }

    public static Movimiento registrar(
            UUID usuarioId,
            UUID jugadorId,
            TipoMovimiento tipo,
            Integer cantidad,
            BigDecimal precioUnitario,
            BigDecimal montoTotal,
            UUID contraparteUsuarioId) {
        return registrar(
                usuarioId,
                jugadorId,
                tipo,
                cantidad,
                precioUnitario,
                montoTotal,
                contraparteUsuarioId,
                Instant.now());
    }

    /** Compra al sistema: se paga la cotización vigente, sin ajuste. */
    public static Movimiento compraSistema(
            UUID usuarioId, UUID jugadorId, int cantidad, BigDecimal cotizacionVigente) {
        Objects.requireNonNull(cotizacionVigente, "cotizacionVigente no puede ser null");
        BigDecimal precioUnitario = Monetario.escalar(cotizacionVigente);
        return registrar(
                usuarioId,
                jugadorId,
                TipoMovimiento.COMPRA_SISTEMA,
                cantidad,
                precioUnitario,
                montoDe(precioUnitario, cantidad),
                null);
    }

    /**
     * Venta al sistema a la cotización vigente menos {@code descuento} ({@code 0.15} = 15%). El
     * precio unitario se redondea antes de multiplicar por la cantidad.
     *
     * @throws IllegalArgumentException si {@code descuento} no está en {@code [0, 1)}
     */
    public static Movimiento ventaSistema(
            UUID usuarioId,
            UUID jugadorId,
            int cantidad,
            BigDecimal cotizacionVigente,
            BigDecimal descuento) {
        Objects.requireNonNull(cotizacionVigente, "cotizacionVigente no puede ser null");
        Objects.requireNonNull(descuento, "descuento no puede ser null");
        if (descuento.signum() < 0 || descuento.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException("descuento debe estar en [0, 1), fue " + descuento);
        }
        BigDecimal precioUnitario =
                Monetario.escalar(cotizacionVigente.multiply(BigDecimal.ONE.subtract(descuento)));
        return registrar(
                usuarioId,
                jugadorId,
                TipoMovimiento.VENTA_SISTEMA,
                cantidad,
                precioUnitario,
                montoDe(precioUnitario, cantidad),
                null);
    }

    private static BigDecimal montoDe(BigDecimal precioUnitario, int cantidad) {
        return Monetario.escalar(precioUnitario.multiply(BigDecimal.valueOf(cantidad)));
    }

    public static Movimiento registrar(
            UUID usuarioId,
            UUID jugadorId,
            TipoMovimiento tipo,
            Integer cantidad,
            BigDecimal precioUnitario,
            BigDecimal montoTotal,
            UUID contraparteUsuarioId,
            Instant fecha) {
        return new Movimiento(
                usuarioId,
                jugadorId,
                tipo,
                cantidad,
                precioUnitario,
                montoTotal,
                contraparteUsuarioId,
                fecha);
    }

    private Movimiento(
            UUID id,
            UUID usuarioId,
            UUID jugadorId,
            TipoMovimiento tipo,
            Integer cantidad,
            BigDecimal precioUnitario,
            BigDecimal montoTotal,
            UUID contraparteUsuarioId,
            Instant fecha) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.jugadorId = jugadorId;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.montoTotal = montoTotal;
        this.contraparteUsuarioId = contraparteUsuarioId;
        this.fecha = fecha;
    }

    /** Reconstruye desde persistencia; no valida las invariantes de alta. */
    public static Movimiento reconstruir(
            UUID id,
            UUID usuarioId,
            UUID jugadorId,
            TipoMovimiento tipo,
            Integer cantidad,
            BigDecimal precioUnitario,
            BigDecimal montoTotal,
            UUID contraparteUsuarioId,
            Instant fecha) {
        return new Movimiento(
                id,
                usuarioId,
                jugadorId,
                tipo,
                cantidad,
                precioUnitario,
                montoTotal,
                contraparteUsuarioId,
                fecha);
    }

    private static void validarCamposObligatorios(
            UUID usuarioId, TipoMovimiento tipo, BigDecimal montoTotal, Instant fecha) {
        Objects.requireNonNull(usuarioId, "usuarioId no puede ser null");
        Objects.requireNonNull(tipo, "tipo no puede ser null");
        Objects.requireNonNull(montoTotal, "montoTotal no puede ser null");
        Objects.requireNonNull(fecha, "fecha no puede ser null");
        exigirPositivo(montoTotal, "montoTotal");
    }

    private static void validarCamposJugador(
            TipoMovimiento tipo, UUID jugadorId, Integer cantidad, BigDecimal precioUnitario) {
        if (tipo == TipoMovimiento.RECARGA_SALDO) {
            exigirNulo(jugadorId, "jugadorId", tipo);
            exigirNulo(cantidad, "cantidad", tipo);
            exigirNulo(precioUnitario, "precioUnitario", tipo);
        } else {
            Objects.requireNonNull(jugadorId, "jugadorId es obligatorio para tipo " + tipo);
            Objects.requireNonNull(cantidad, "cantidad es obligatoria para tipo " + tipo);
            Objects.requireNonNull(
                    precioUnitario, "precioUnitario es obligatorio para tipo " + tipo);
            exigirPositivo(cantidad, "cantidad");
            exigirPositivo(precioUnitario, "precioUnitario");
        }
    }

    private static void validarContraparte(TipoMovimiento tipo, UUID contraparteUsuarioId) {
        if (tipo == TipoMovimiento.COMPRA_P2P
                || tipo == TipoMovimiento.VENTA_P2P
                || tipo == TipoMovimiento.COMPRA_SUBASTA
                || tipo == TipoMovimiento.VENTA_SUBASTA) {
            Objects.requireNonNull(
                    contraparteUsuarioId, "contraparteUsuarioId es obligatorio para tipo " + tipo);
        } else {
            exigirNulo(contraparteUsuarioId, "contraparteUsuarioId", tipo);
        }
    }

    private static void exigirNulo(Object valor, String nombreCampo, TipoMovimiento tipo) {
        if (valor != null) {
            throw new IllegalArgumentException(nombreCampo + " debe ser null para tipo " + tipo);
        }
    }

    private static void exigirPositivo(BigDecimal valor, String nombreCampo) {
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException(
                    nombreCampo + " debe ser mayor a cero, fue " + valor);
        }
    }

    private static void exigirPositivo(Integer valor, String nombreCampo) {
        if (valor <= 0) {
            throw new IllegalArgumentException(
                    nombreCampo + " debe ser mayor a cero, fue " + valor);
        }
    }
}
