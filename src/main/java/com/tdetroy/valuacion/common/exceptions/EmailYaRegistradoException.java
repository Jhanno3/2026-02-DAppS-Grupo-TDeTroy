package com.tdetroy.valuacion.common.exceptions;

/**
 * Ya existe una cuenta con el email solicitado.
 *
 * <p>Lanzada por {@code UsuarioServiceImpl.registrar} (plan.md §2.1, UC-01: "El sistema valida que
 * el identificador no esté ya registrado") tras consultar {@code UsuarioRepository} — la unicidad
 * en sí la protege además la constraint {@code uk_usuarios_email} a nivel de esquema
 * (V4__usuarios.sql) como última línea de defensa.
 */
public final class EmailYaRegistradoException extends NegocioException {

    public EmailYaRegistradoException(String email) {
        super("Ya existe una cuenta registrada con el email " + email);
    }
}
