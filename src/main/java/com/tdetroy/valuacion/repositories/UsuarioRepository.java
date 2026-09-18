package com.tdetroy.valuacion.repositories;

import com.tdetroy.valuacion.model.Usuario;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA sobre {@link Usuario} (plan.md §2.1). {@link #existsByEmail} lo
 * agrega {@code UsuarioServiceImpl.registrar} (T1.3, UC-01: "El sistema valida que el identificador
 * no esté ya registrado"); {@link #findByEmail} lo agrega {@code UsuarioUserDetailsService} (T1.4,
 * {@code config/}, UC-02) para resolver el login — mismo criterio de no anticipar consultas que
 * {@link MovimientoRepository}/{@link RegistroAuditoriaRepository}, sólo que ya se cumplió dos
 * veces. La unicidad de {@code email} está protegida además a nivel de esquema por la constraint
 * {@code uk_usuarios_email} (V4__usuarios.sql).
 */
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    boolean existsByEmail(String email);

    Optional<Usuario> findByEmail(String email);
}
