package com.tdetroy.valuacion.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** {@code POST /auth/login} (UC-02). Bean Validation en el borde del Controller. */
public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
