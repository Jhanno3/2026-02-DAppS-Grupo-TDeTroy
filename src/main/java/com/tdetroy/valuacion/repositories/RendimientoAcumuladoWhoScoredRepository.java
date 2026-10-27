package com.tdetroy.valuacion.repositories;

import com.tdetroy.valuacion.entity.RendimientoAcumuladoWhoScoredEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA sobre {@link RendimientoAcumuladoWhoScoredEntity} (plan.md §8.1).
 * {@code jugadorId} es la clave primaria natural (snapshot 1:1 por jugador) — {@link
 * #findById(Object)} ya alcanza para que {@code RendimientoServiceImpl} resuelva "¿hay un snapshot
 * previo de este jugador?" sin necesitar una consulta propia.
 */
public interface RendimientoAcumuladoWhoScoredRepository
        extends JpaRepository<RendimientoAcumuladoWhoScoredEntity, UUID> {}
