package com.tdetroy.valuacion.repositories;

import com.tdetroy.valuacion.entity.TenenciaTokenEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA sobre {@link TenenciaTokenEntity} (plan.md §2.5). {@link
 * #findByUsuarioIdAndJugadorId} (T5.1) es la primera consulta propia, agregada por {@code
 * TokenServiceImpl} para resolver la tenencia de un par usuario/jugador antes de acreditarla
 * (primera compra) o debitarla (venta). {@link #findByUsuarioIdAndCantidadGreaterThan} (T5.3) la
 * agrega {@code PortfolioServiceImpl} para listar las tenencias de un usuario con {@code cantidad >
 * 0} (UC-11: un usuario que vendió toda su tenencia de un jugador no debe seguir apareciendo en su
 * portfolio, aunque la fila siga existiendo con {@code cantidad = 0}) — mismo criterio que {@link
 * MovimientoRepository} para no anticipar una forma de consulta que todavía no se pidió. La
 * unicidad del par {@code usuarioId}/{@code jugadorId} está protegida a nivel de esquema por la
 * constraint {@code uk_tenencias_token_usuario_jugador} (V6__tenencias_token.sql).
 */
public interface TenenciaTokenRepository extends JpaRepository<TenenciaTokenEntity, UUID> {

    Optional<TenenciaTokenEntity> findByUsuarioIdAndJugadorId(UUID usuarioId, UUID jugadorId);

    List<TenenciaTokenEntity> findByUsuarioIdAndCantidadGreaterThan(UUID usuarioId, int cantidad);
}
