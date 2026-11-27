package com.tdetroy.valuacion.repositories.cotizacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.tdetroy.valuacion.repositories.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

class IndicesCotizacionIT extends PostgresIntegrationTest {

    @Autowired private TestEntityManager entityManager;

    @Test
    void laMigracionCreaElIndicePorJugadorSemanaYFecha() {
        Object definicion =
                entityManager
                        .getEntityManager()
                        .createNativeQuery(
                                "SELECT indexdef FROM pg_indexes WHERE indexname ="
                                        + " 'idx_cotizaciones_historicas_jugador_semana_fecha'")
                        .getSingleResult();

        assertThat(definicion.toString())
                .contains("cotizaciones_historicas")
                .contains("(jugador_id, semana, fecha_calculo DESC)");
    }
}
