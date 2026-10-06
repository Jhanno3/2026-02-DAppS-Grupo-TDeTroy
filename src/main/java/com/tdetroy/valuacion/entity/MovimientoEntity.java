package com.tdetroy.valuacion.entity;

import com.tdetroy.valuacion.model.Movimiento;
import com.tdetroy.valuacion.model.TipoMovimiento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad JPA sobre la tabla {@code movimientos} (plan.md §2.6). Refleja el esquema 1:1, sin lógica
 * de negocio ni invariantes propias — eso vive en {@link Movimiento} (constitution.md §2).
 */
@Entity
@Table(name = "movimientos")
@Getter
@Setter
@NoArgsConstructor
public class MovimientoEntity {

    @Id private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID usuarioId;

    @Column(updatable = false)
    private UUID jugadorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private TipoMovimiento tipo;

    @Column(updatable = false)
    private Integer cantidad;

    @Column(updatable = false, precision = 19, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal montoTotal;

    @Column(updatable = false)
    private UUID contraparteUsuarioId;

    @Column(nullable = false, updatable = false)
    private Instant fecha;

    /** Traduce {@code movimiento} a la fila que persiste {@code repositories/}. */
    public static MovimientoEntity desde(Movimiento movimiento) {
        MovimientoEntity entity = new MovimientoEntity();
        entity.id = movimiento.getId();
        entity.usuarioId = movimiento.getUsuarioId();
        entity.jugadorId = movimiento.getJugadorId();
        entity.tipo = movimiento.getTipo();
        entity.cantidad = movimiento.getCantidad();
        entity.precioUnitario = movimiento.getPrecioUnitario();
        entity.montoTotal = movimiento.getMontoTotal();
        entity.contraparteUsuarioId = movimiento.getContraparteUsuarioId();
        entity.fecha = movimiento.getFecha();
        return entity;
    }

    /** Reconstruye el objeto de dominio a partir de esta fila ya persistida. */
    public Movimiento aModelo() {
        return Movimiento.reconstruir(
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
}
