package com.tdetroy.valuacion.dto.response;

/**
 * Respuesta de {@code POST /auth/login} (UC-02, plan.md §3: "devuelve JWT"). Se llama {@code token}
 * y no {@code jwt} a secas para no acoplar el nombre del campo público de la API a la tecnología
 * concreta de sesión elegida.
 */
public record LoginResponse(String token) {}
