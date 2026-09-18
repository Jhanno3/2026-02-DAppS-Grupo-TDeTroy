package com.tdetroy.valuacion.dto.response;

import com.tdetroy.valuacion.model.RolUsuario;
import com.tdetroy.valuacion.model.Usuario;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Respuesta de {@code POST /auth/registro} (UC-01). Nunca incluye {@code passwordHash}
 * (constitution.md §4) — {@link Usuario} nunca cruza el borde del Controller directamente
 * (constitution.md §1/§2).
 */
public record UsuarioResponse(
        UUID id, String email, RolUsuario rol, BigDecimal saldoVirtual, Instant fechaCreacion) {

    public static UsuarioResponse desde(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getRol(),
                usuario.getSaldoVirtual(),
                usuario.getFechaCreacion());
    }
}
