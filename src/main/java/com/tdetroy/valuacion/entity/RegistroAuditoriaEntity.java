package com.tdetroy.valuacion.entity;

import com.tdetroy.valuacion.model.RegistroAuditoria;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entidad JPA sobre la tabla {@code registros_auditoria} (plan.md §2.8/§10). Refleja el esquema
 * 1:1, sin lógica de negocio ni invariantes propias — eso vive en {@link RegistroAuditoria}
 * (constitution.md §2).
 */
@Entity
@Table(name = "registros_auditoria")
@Getter
@Setter
@NoArgsConstructor
public class RegistroAuditoriaEntity {

    @Id private UUID id;

    @Column(updatable = false)
    private UUID actorId;

    @Column(nullable = false, updatable = false)
    private String accion;

    @Column(nullable = false, updatable = false)
    private String entidadAfectada;

    @Column(nullable = false, updatable = false)
    private UUID entidadId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(updatable = false)
    private String valoresAntes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false)
    private String valoresDespues;

    @Column(nullable = false, updatable = false)
    private Instant fecha;

    /** Traduce {@code registro} a la fila que persiste {@code repositories/}. */
    public static RegistroAuditoriaEntity desde(RegistroAuditoria registro) {
        RegistroAuditoriaEntity entity = new RegistroAuditoriaEntity();
        entity.id = registro.getId();
        entity.actorId = registro.getActorId();
        entity.accion = registro.getAccion();
        entity.entidadAfectada = registro.getEntidadAfectada();
        entity.entidadId = registro.getEntidadId();
        entity.valoresAntes = registro.getValoresAntes();
        entity.valoresDespues = registro.getValoresDespues();
        entity.fecha = registro.getFecha();
        return entity;
    }

    /** Reconstruye el objeto de dominio a partir de esta fila ya persistida. */
    public RegistroAuditoria aModelo() {
        return RegistroAuditoria.reconstruir(
                id,
                actorId,
                accion,
                entidadAfectada,
                entidadId,
                valoresAntes,
                valoresDespues,
                fecha);
    }
}
