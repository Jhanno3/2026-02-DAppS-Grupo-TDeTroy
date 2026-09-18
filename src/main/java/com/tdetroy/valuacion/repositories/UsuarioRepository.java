package com.tdetroy.valuacion.repositories;

import com.tdetroy.valuacion.model.Usuario;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA sobre {@link Usuario} (plan.md §2.1). {@link #existsByEmail} lo
 * agrega {@code UsuarioServiceImpl.registrar} (T1.3, UC-01: "El sistema valida que el identificador
 * no esté ya registrado"); no se agrega ningún otro método de consulta hasta que un Service lo
 * necesite (ej. la búsqueda por email para resolver el login, UC-02, la agregará T1.4) — mismo
 * criterio que {@link MovimientoRepository}/{@link RegistroAuditoriaRepository}. La unicidad de
 * {@code email} está protegida además a nivel de esquema por la constraint {@code
 * uk_usuarios_email} (V4__usuarios.sql).
 */
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    boolean existsByEmail(String email);
}
