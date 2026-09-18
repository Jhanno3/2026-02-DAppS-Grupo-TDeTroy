package com.tdetroy.valuacion.services;

import java.util.UUID;

/**
 * Escritura del log de auditoría interno (plan.md §2.8/§10, constitution.md §4). Único punto por el
 * que se crea un {@code RegistroAuditoria} — ningún otro Service persiste esa entidad directamente
 * vía {@code RegistroAuditoriaRepository}.
 *
 * <p>Se invoca desde dentro de la misma transacción de negocio que la operación auditada (plan.md
 * §10): quien orquesta esa transacción (ej. {@code JugadorService}, {@code CotizacionService},
 * {@code OfertaService}) es responsable de llamar a este método, no al revés.
 */
public interface AuditoriaService {

    /**
     * Registra una entrada de auditoría.
     *
     * @param actorId quién ejecutó la acción; {@code null} = SISTEMA (ej. job semanal)
     * @param accion qué se hizo (ej. {@code "BAJA_JUGADOR"}, {@code "ALTA_JUGADOR"})
     * @param entidadAfectada tipo de entidad afectada (ej. {@code "Jugador"})
     * @param entidadId id de la entidad afectada
     * @param valoresAntes estado previo a serializar a JSON; {@code null} si no hay estado previo
     *     (ej. alta de una entidad nueva)
     * @param valoresDespues estado posterior a serializar a JSON; nunca {@code null}
     */
    void registrar(
            UUID actorId,
            String accion,
            String entidadAfectada,
            UUID entidadId,
            Object valoresAntes,
            Object valoresDespues);
}
