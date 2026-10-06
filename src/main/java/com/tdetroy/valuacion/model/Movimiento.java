package com.tdetroy.valuacion.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * Registro de un movimiento de cara al usuario (UC-12, plan.md §2.6): recarga de saldo, compra o
 * venta contra el sistema, compra o venta P2P, o compensación por baja de un jugador.
 *
 * <p><b>Append-only:</b> no expone ningún método de mutación ni setter público — se construye
 * completo con {@link #registrar} y queda inmutable desde ese momento (constitution.md §2). Es
 * distinto del log de auditoría interno ({@code RegistroAuditoria}, plan.md §10), que además cubre
 * operaciones administrativas sin usuario final asociado.
 *
 * <p>La combinación válida de campos según {@link #tipo} es una invariante propia de esta entidad,
 * protegida en el único punto por el que se puede crear un {@code Movimiento} — el constructor
 * privado invocado desde {@link #registrar} — nunca sólo validada en el Service que lo invoca
 * (constitution.md §2):
 *
 * <ul>
 *   <li>{@link TipoMovimiento#RECARGA_SALDO}: {@code jugadorId}, {@code cantidad}, {@code
 *       precioUnitario} y {@code contraparteUsuarioId} deben ser {@code null}.
 *   <li>Cualquier otro tipo: {@code jugadorId}, {@code cantidad} (&gt;0) y {@code precioUnitario}
 *       (&gt;0) son obligatorios.
 *   <li>{@code contraparteUsuarioId} es obligatorio únicamente para {@link
 *       TipoMovimiento#COMPRA_P2P}/{@link TipoMovimiento#VENTA_P2P}, y debe ser {@code null} para
 *       el resto.
 *   <li>{@code montoTotal} siempre es obligatorio y mayor a cero.
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

    /**
     * Registra un nuevo movimiento con {@code fecha = Instant.now()}. Único punto de creación de
     * {@code Movimiento} — no hay otro constructor público ni setters.
     */
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

    /** Igual que {@link #registrar}, con {@code fecha} explícita (tests, reproducibilidad). */
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

    /**
     * Reconstruye un {@code Movimiento} ya persistido a partir de sus datos crudos (usado por
     * {@code entity/MovimientoEntity#aModelo()} en el límite con {@code repositories/}) — a
     * diferencia de {@link #registrar}, no valida las invariantes de alta.
     */
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
        requirePositivo(montoTotal, "montoTotal");
    }

    private static void validarCamposJugador(
            TipoMovimiento tipo, UUID jugadorId, Integer cantidad, BigDecimal precioUnitario) {
        if (tipo == TipoMovimiento.RECARGA_SALDO) {
            requireNull(jugadorId, "jugadorId", tipo);
            requireNull(cantidad, "cantidad", tipo);
            requireNull(precioUnitario, "precioUnitario", tipo);
        } else {
            Objects.requireNonNull(jugadorId, "jugadorId es obligatorio para tipo " + tipo);
            Objects.requireNonNull(cantidad, "cantidad es obligatoria para tipo " + tipo);
            Objects.requireNonNull(
                    precioUnitario, "precioUnitario es obligatorio para tipo " + tipo);
            requirePositivo(cantidad, "cantidad");
            requirePositivo(precioUnitario, "precioUnitario");
        }
    }

    private static void validarContraparte(TipoMovimiento tipo, UUID contraparteUsuarioId) {
        if (tipo == TipoMovimiento.COMPRA_P2P || tipo == TipoMovimiento.VENTA_P2P) {
            Objects.requireNonNull(
                    contraparteUsuarioId, "contraparteUsuarioId es obligatorio para tipo " + tipo);
        } else {
            requireNull(contraparteUsuarioId, "contraparteUsuarioId", tipo);
        }
    }

    private static void requireNull(Object valor, String nombreCampo, TipoMovimiento tipo) {
        if (valor != null) {
            throw new IllegalArgumentException(nombreCampo + " debe ser null para tipo " + tipo);
        }
    }

    private static void requirePositivo(BigDecimal valor, String nombreCampo) {
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException(
                    nombreCampo + " debe ser mayor a cero, fue " + valor);
        }
    }

    private static void requirePositivo(Integer valor, String nombreCampo) {
        if (valor <= 0) {
            throw new IllegalArgumentException(
                    nombreCampo + " debe ser mayor a cero, fue " + valor);
        }
    }
}
