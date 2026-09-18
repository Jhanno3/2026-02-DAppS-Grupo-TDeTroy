package com.tdetroy.valuacion.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * {@code POST /auth/registro} (UC-01). Bean Validation en el borde del Controller (constitution.md
 * §4) — la unicidad de {@code email} no se valida acá, es una regla de negocio que resuelve {@code
 * UsuarioService} (T1.3) contra la base.
 */
public record RegistrarUsuarioRequest(@NotBlank @Email String email, @NotBlank String password) {}
