package com.tdetroy.valuacion.model;

/**
 * Rol de un {@link Usuario} (plan.md §2.1). Toda cuenta creada vía {@link Usuario#registrar} nace
 * {@link #USER} (UC-01); no existe alta pública de {@link #ADMIN} en el MVP.
 */
public enum RolUsuario {
    USER,
    ADMIN
}
