package com.tdetroy.valuacion.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * Log de auditoría interno, append-only e inmutable (plan.md §2.8, §10; constitution.md §4).
 *
 * <p>Distinto de {@link Movimiento} (de cara al usuario, UC-12): cubre además operaciones
 * administrativas sin usuario final directo (alta/edición/baja de jugador, disparo manual de
 * recálculo). No tiene endpoint público en el MVP — se escribe siempre a través de {@code
 * AuditoriaService} y se consulta directamente en base.
 *
 * <p>{@code valoresAntes}/{@code valoresDespues} guardan JSON ya serializado como texto plano,
 * mapeado a una columna {@code jsonb} por {@code entity/RegistroAuditoriaEntity}. La entidad nunca
 * serializa: eso es trabajo de {@code AuditoriaService} (capa de negocio), para no acoplar {@code
 * model/} a ninguna librería JSON concreta.
 */
@Getter
public class RegistroAuditoria {

    private final UUID id;

    /** {@code null} = SISTEMA (ej. job semanal de recotización, plan.md §2.8). */
    private final UUID actorId;

    private final String accion;
    private final String entidadAfectada;
    private final UUID entidadId;
    private final String valoresAntes;
    private final String valoresDespues;
    private final Instant fecha;

    private RegistroAuditoria(
            UUID actorId,
            String accion,
            String entidadAfectada,
            UUID entidadId,
            String valoresAntes,
            String valoresDespues,
            Instant fecha) {
        validarCampos(accion, entidadAfectada, entidadId, valoresDespues, fecha);

        this.id = UUID.randomUUID();
        this.actorId = actorId;
        this.accion = accion;
        this.entidadAfectada = entidadAfectada;
        this.entidadId = entidadId;
        this.valoresAntes = valoresAntes;
        this.valoresDespues = valoresDespues;
        this.fecha = fecha;
    }

    /** Registra una entrada con {@code fecha = Instant.now()}. */
    public static RegistroAuditoria registrar(
            UUID actorId,
            String accion,
            String entidadAfectada,
            UUID entidadId,
            String valoresAntes,
            String valoresDespues) {
        return registrar(
                actorId,
                accion,
                entidadAfectada,
                entidadId,
                valoresAntes,
                valoresDespues,
                Instant.now());
    }

    /** Igual que {@link #registrar}, con {@code fecha} explícita (tests, reproducibilidad). */
    public static RegistroAuditoria registrar(
            UUID actorId,
            String accion,
            String entidadAfectada,
            UUID entidadId,
            String valoresAntes,
            String valoresDespues,
            Instant fecha) {
        return new RegistroAuditoria(
                actorId, accion, entidadAfectada, entidadId, valoresAntes, valoresDespues, fecha);
    }

    private RegistroAuditoria(
            UUID id,
            UUID actorId,
            String accion,
            String entidadAfectada,
            UUID entidadId,
            String valoresAntes,
            String valoresDespues,
            Instant fecha) {
        this.id = id;
        this.actorId = actorId;
        this.accion = accion;
        this.entidadAfectada = entidadAfectada;
        this.entidadId = entidadId;
        this.valoresAntes = valoresAntes;
        this.valoresDespues = valoresDespues;
        this.fecha = fecha;
    }

    /**
     * Reconstruye un {@code RegistroAuditoria} ya persistido a partir de sus datos crudos (usado
     * por {@code entity/RegistroAuditoriaEntity#aModelo()} en el límite con {@code repositories/})
     * — a diferencia de {@link #registrar}, no valida las invariantes de alta.
     */
    public static RegistroAuditoria reconstruir(
            UUID id,
            UUID actorId,
            String accion,
            String entidadAfectada,
            UUID entidadId,
            String valoresAntes,
            String valoresDespues,
            Instant fecha) {
        return new RegistroAuditoria(
                id,
                actorId,
                accion,
                entidadAfectada,
                entidadId,
                valoresAntes,
                valoresDespues,
                fecha);
    }

    private static void validarCampos(
            String accion,
            String entidadAfectada,
            UUID entidadId,
            String valoresDespues,
            Instant fecha) {
        Objects.requireNonNull(accion, "accion no puede ser null");
        Objects.requireNonNull(entidadAfectada, "entidadAfectada no puede ser null");
        Objects.requireNonNull(entidadId, "entidadId no puede ser null");
        Objects.requireNonNull(valoresDespues, "valoresDespues no puede ser null");
        Objects.requireNonNull(fecha, "fecha no puede ser null");
        requireNoBlank(accion, "accion");
        requireNoBlank(entidadAfectada, "entidadAfectada");
    }

    private static void requireNoBlank(String valor, String nombreCampo) {
        if (valor.isBlank()) {
            throw new IllegalArgumentException(nombreCampo + " no puede estar vacío ni ser blanco");
        }
    }
}
