package com.tdetroy.valuacion.model.auditoria;

import com.tdetroy.valuacion.model.cartera.Movimiento;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * Log de auditoría interno, append-only. A diferencia de {@link Movimiento} (de cara al usuario),
 * cubre también operaciones administrativas. Se escribe solo a través de {@code AuditoriaService}.
 *
 * <p>Los valores llegan como JSON ya serializado, para no acoplar {@code model/} a una librería
 * JSON.
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

    /** Reconstruye desde persistencia; no valida las invariantes de alta. */
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
        exigirNoVacio(accion, "accion");
        exigirNoVacio(entidadAfectada, "entidadAfectada");
    }

    private static void exigirNoVacio(String valor, String nombreCampo) {
        if (valor.isBlank()) {
            throw new IllegalArgumentException(nombreCampo + " no puede estar vacío ni ser blanco");
        }
    }
}
