package com.tdetroy.valuacion.common.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * {@link RecursoNoEncontradoException} es deliberadamente independiente de {@link NegocioException}
 * (404 vs. 409, ver javadoc de la clase) — se verifica acá por separado de {@link
 * ExcepcionesNegocioTest}.
 */
class RecursoNoEncontradoExceptionTest {

    @Test
    void noEsNegocioExceptionYArmaElMensajeConTipoYId() {
        UUID id = UUID.randomUUID();

        var ex = new RecursoNoEncontradoException("Usuario", id);

        assertThat(ex).isNotInstanceOf(NegocioException.class);
        assertThat(ex.getMessage()).contains("Usuario").contains(id.toString());
    }
}
