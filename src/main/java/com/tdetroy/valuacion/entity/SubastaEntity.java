package com.tdetroy.valuacion.entity;

import com.tdetroy.valuacion.model.EstadoSubasta;
import com.tdetroy.valuacion.model.Subasta;
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
 * Entidad JPA sobre la tabla {@code subastas} (UC-18). Refleja el esquema 1:1, sin lógica de
 * negocio — eso vive en {@link Subasta} (constitution.md §2).
 */
@Entity
@Table(name = "subastas")
@Getter
@Setter
@NoArgsConstructor
public class SubastaEntity {

    @Id private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID vendedorId;

    @Column(nullable = false, updatable = false)
    private UUID jugadorId;

    @Column(nullable = false, updatable = false)
    private int cantidad;

    @Column(nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal precioInicial;

    @Column(precision = 19, scale = 2)
    private BigDecimal precioActual;

    private UUID mejorPostorId;

    @Column(nullable = false)
    private int cantidadPujas;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoSubasta estado;

    @Column(nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(nullable = false, updatable = false)
    private Instant fechaVencimiento;

    private Instant fechaCierre;

    /** Traduce {@code subasta} a la fila que persiste {@code repositories/}. */
    public static SubastaEntity desde(Subasta subasta) {
        SubastaEntity entity = new SubastaEntity();
        entity.id = subasta.getId();
        entity.vendedorId = subasta.getVendedorId();
        entity.jugadorId = subasta.getJugadorId();
        entity.cantidad = subasta.getCantidad();
        entity.precioInicial = subasta.getPrecioInicial();
        entity.precioActual = subasta.getPrecioActual();
        entity.mejorPostorId = subasta.getMejorPostorId();
        entity.cantidadPujas = subasta.getCantidadPujas();
        entity.estado = subasta.getEstado();
        entity.fechaCreacion = subasta.getFechaCreacion();
        entity.fechaVencimiento = subasta.getFechaVencimiento();
        entity.fechaCierre = subasta.getFechaCierre();
        return entity;
    }

    /** Reconstruye el objeto de dominio a partir de esta fila ya persistida. */
    public Subasta aModelo() {
        return Subasta.reconstruir(
                id,
                vendedorId,
                jugadorId,
                cantidad,
                precioInicial,
                precioActual,
                mejorPostorId,
                cantidadPujas,
                estado,
                fechaCreacion,
                fechaVencimiento,
                fechaCierre);
    }
}
