package com.tdetroy.valuacion.repositories;

import com.tdetroy.valuacion.entity.OfertaP2PEntity;
import com.tdetroy.valuacion.model.EstadoOferta;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

/**
 * Repositorio Spring Data JPA sobre {@link OfertaP2PEntity} (plan.md §2.7). {@link #findById(UUID)}
 * está sobrescrito con {@link LockModeType#PESSIMISTIC_WRITE} (plan.md §9.3, {@code SELECT ... FOR
 * UPDATE}): es el punto de lectura que {@code OfertaServiceImpl.ejecutar()}/{@code cancelar()}
 * (T6.4/T6.5) usan dentro de la transacción que muta la oferta — garantiza que, si dos compradores
 * compiten por la misma oferta al mismo tiempo, el segundo espera a que el primero termine su
 * transacción en vez de leer un {@code cantidadDisponible} ya obsoleto (ver javadoc de {@link
 * OfertaP2PEntity} y constitution.md §2: "nunca se vende más cantidad que la publicada"). {@link
 * #findByJugadorIdAndEstado} (T6.3/T6.6) la agregan {@code OfertaController} (listado de ofertas
 * abiertas de un jugador, sin lock: es sólo para mostrarle al comprador qué hay disponible) y
 * {@code JugadorServiceImpl.darDeBaja} (para encontrar qué ofertas de ese jugador cancelar
 * automáticamente, UC-15); {@link #findByEstado} la agrega {@code OfertaController} para el listado
 * global de ofertas abiertas de todo el catálogo (sin filtrar por jugador, "pestaña P2P"); {@link
 * #findByEstadoAndFechaVencimientoBefore} la agrega {@code OfertaServiceImpl.cancelarVencidas} para
 * el chequeo periódico de ofertas vencidas (UC-17) — mismo criterio que {@code
 * JugadorRepository}/{@code MovimientoRepository} de no anticipar una forma de consulta que todavía
 * no se pidió.
 */
public interface OfertaP2PRepository extends JpaRepository<OfertaP2PEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OfertaP2PEntity> findById(UUID id);

    List<OfertaP2PEntity> findByJugadorIdAndEstado(UUID jugadorId, EstadoOferta estado);

    List<OfertaP2PEntity> findByEstado(EstadoOferta estado);

    List<OfertaP2PEntity> findByEstadoAndFechaVencimientoBefore(
            EstadoOferta estado, Instant instante);
}
