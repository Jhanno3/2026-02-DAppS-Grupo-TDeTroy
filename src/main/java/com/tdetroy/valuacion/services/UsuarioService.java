package com.tdetroy.valuacion.services;

import com.tdetroy.valuacion.common.exceptions.EmailYaRegistradoException;
import com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException;
import com.tdetroy.valuacion.model.Usuario;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Registro de cuentas y movimiento de saldo virtual (plan.md §1, UC-01/UC-14).
 *
 * <p>Responsabilidad acotada a {@link Usuario}: no conoce JWT, roles como {@code GrantedAuthority},
 * ni ningún otro detalle de autenticación/autorización — eso vive en {@code config/} (T1.4), que
 * consume {@code UsuarioRepository} directamente para resolver el login (constitution.md §2:
 * prohibido el acoplamiento inverso, pero un componente de infraestructura como {@code
 * UserDetailsService} leyendo un Repository no es una capa {@code Service} de negocio).
 */
public interface UsuarioService {

    /**
     * Da de alta una cuenta {@code USER} con saldo inicial cero (UC-01).
     *
     * @param passwordHash ya hasheado (BCrypt) por quien invoca — este Service nunca ve la
     *     contraseña en texto plano
     * @throws EmailYaRegistradoException si {@code email} ya tiene una cuenta registrada
     */
    Usuario registrar(String email, String passwordHash);

    /**
     * Acredita {@code monto} al saldo virtual de un usuario existente, registra el {@code
     * Movimiento} correspondiente y audita la operación (UC-14).
     *
     * @param actorId quién ejecutó la recarga (el ADMIN autenticado)
     * @throws RecursoNoEncontradoException si no existe un usuario con {@code usuarioId}
     * @throws IllegalArgumentException si {@code monto} no es mayor a cero
     */
    Usuario recargarSaldo(UUID usuarioId, BigDecimal monto, UUID actorId);
}
