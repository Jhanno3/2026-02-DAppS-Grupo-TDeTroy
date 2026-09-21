package com.tdetroy.valuacion.repositories;

import com.tdetroy.valuacion.model.TenenciaToken;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA sobre {@link TenenciaToken} (plan.md §2.5). Sin métodos de consulta
 * propios todavía: {@code TokenServiceImpl}/{@code PortfolioServiceImpl} (T5.1/T5.3) agregan las
 * consultas que efectivamente necesiten (ej. buscar por {@code usuarioId}+{@code jugadorId} antes
 * de acreditar, listar tenencias de un usuario para el portfolio), mismo criterio que {@link
 * MovimientoRepository} para no anticipar una forma de consulta que todavía no se pidió. La
 * unicidad del par {@code usuarioId}/{@code jugadorId} está protegida a nivel de esquema por la
 * constraint {@code uk_tenencias_token_usuario_jugador} (V6__tenencias_token.sql).
 */
public interface TenenciaTokenRepository extends JpaRepository<TenenciaToken, UUID> {}
