package com.tdetroy.valuacion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Registro de un movimiento de cara al usuario (UC-12, plan.md §2.6): recarga de saldo, compra
 * o venta contra el sistema, compra o venta P2P, o compensación por baja de un jugador.
 *
 * <p><b>Append-only:</b> no expone ningún método de mutación ni setter público — se construye
 * completo con {@link #registrar} y queda inmutable desde ese momento (constitution.md §2). Es
 * distinto del log de auditoría interno ({@code RegistroAuditoria}, plan.md §10), que además
 * cubre operaciones administrativas sin usuario final asociado.
 *
 * <p>La combinación válida de campos según {@link #tipo} es una invariante propia de esta
 * entidad, protegida en el único punto por el que se puede crear un {@code Movimiento} — el
 * constructor privado invocado desde {@link #registrar} — nunca sólo validada en el Service que
 * lo invoca (constitution.md §2):
 * <ul>
 *   <li>{@link TipoMovimiento#RECARGA_SALDO}: {@code jugadorId}, {@code cantidad},
 *       {@code precioUnitario} y {@code contraparteUsuarioId} deben ser {@code null}.</li>
 *   <li>Cualquier otro tipo: {@code jugadorId}, {@code cantidad} (&gt;0) y
 *       {@code precioUnitario} (&gt;0) son obligatorios.</li>
 *   <li>{@code contraparteUsuarioId} es obligatorio únicamente para
 *       {@link TipoMovimiento#COMPRA_P2P}/{@link TipoMovimiento#VENTA_P2P}, y debe ser
 *       {@code null} para el resto.</li>
 *   <li>{@code montoTotal} siempre es obligatorio y mayor a cero.</li>
 * </ul>
 */
@Entity
@Table(name = "movimientos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class Movimiento {

    @Id
    private final UUID id;

    @Column(nullable = false, updatable = false)
    private final UUID usuarioId;

    @Column(updatable = false)
    private final UUID jugadorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private final TipoMovimiento tipo;

    @Column(updatable = false)
    private final Integer cantidad;

    @Column(updatable = false, precision = 19, scale = 2)
    private final BigDecimal precioUnitario;

    @Column(nullable = false, updatable = false, precision = 19, scale = 2)
    private final BigDecimal montoTotal;

    @Column(updatable = false)
    private final UUID contraparteUsuarioId;

    @Column(nullable = false, updatable = false)
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
        Objects.requireNonNull(usuarioId, "usuarioId no puede ser null");
        Objects.requireNonNull(tipo, "tipo no puede ser null");
        Objects.requireNonNull(montoTotal, "montoTotal no puede ser null");
        Objects.requireNonNull(fecha, "fecha no puede ser null");
        requirePositivo(montoTotal, "montoTotal");

        if (tipo == TipoMovimiento.RECARGA_SALDO) {
            requireNull(jugadorId, "jugadorId", tipo);
            requireNull(cantidad, "cantidad", tipo);
            requireNull(precioUnitario, "precioUnitario", tipo);
        } else {
            Objects.requireNonNull(jugadorId, "jugadorId es obligatorio para tipo " + tipo);
            Objects.requireNonNull(cantidad, "cantidad es obligatoria para tipo " + tipo);
            Objects.requireNonNull(precioUnitario, "precioUnitario es obligatorio para tipo " + tipo);
            requirePositivo(cantidad, "cantidad");
            requirePositivo(precioUnitario, "precioUnitario");
        }

        if (tipo == TipoMovimiento.COMPRA_P2P || tipo == TipoMovimiento.VENTA_P2P) {
            Objects.requireNonNull(
                    contraparteUsuarioId, "contraparteUsuarioId es obligatorio para tipo " + tipo);
        } else {
            requireNull(contraparteUsuarioId, "contraparteUsuarioId", tipo);
        }

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
                usuarioId, jugadorId, tipo, cantidad, precioUnitario, montoTotal, contraparteUsuarioId, Instant.now());
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
                usuarioId, jugadorId, tipo, cantidad, precioUnitario, montoTotal, contraparteUsuarioId, fecha);
    }

    private static void requireNull(Object valor, String nombreCampo, TipoMovimiento tipo) {
        if (valor != null) {
            throw new IllegalArgumentException(nombreCampo + " debe ser null para tipo " + tipo);
        }
    }

    private static void requirePositivo(BigDecimal valor, String nombreCampo) {
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException(nombreCampo + " debe ser mayor a cero, fue " + valor);
        }
    }

    private static void requirePositivo(Integer valor, String nombreCampo) {
        if (valor <= 0) {
            throw new IllegalArgumentException(nombreCampo + " debe ser mayor a cero, fue " + valor);
        }
    }
}
