package com.tdetroy.valuacion.services;

import com.tdetroy.valuacion.model.RegistroAuditoria;
import com.tdetroy.valuacion.repositories.RegistroAuditoriaRepository;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * {@inheritDoc}
 *
 * <p>Serializa {@code valoresAntes}/{@code valoresDespues} a JSON acá, en la capa de negocio —
 * {@code model/RegistroAuditoria} nunca conoce Jackson ni ninguna otra librería de serialización
 * (constitution.md §2: {@code Model} no conoce a {@code Service}, tampoco al revés más allá de
 * recibir tipos simples).
 */
@Service
public class AuditoriaServiceImpl implements AuditoriaService {

    private final RegistroAuditoriaRepository repository;
    private final ObjectMapper objectMapper;

    public AuditoriaServiceImpl(RegistroAuditoriaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void registrar(
            UUID actorId,
            String accion,
            String entidadAfectada,
            UUID entidadId,
            Object valoresAntes,
            Object valoresDespues) {
        Objects.requireNonNull(valoresDespues, "valoresDespues no puede ser null");

        String jsonAntes =
                valoresAntes == null ? null : objectMapper.writeValueAsString(valoresAntes);
        String jsonDespues = objectMapper.writeValueAsString(valoresDespues);

        RegistroAuditoria registro =
                RegistroAuditoria.registrar(
                        actorId, accion, entidadAfectada, entidadId, jsonAntes, jsonDespues);
        repository.save(registro);
    }
}
