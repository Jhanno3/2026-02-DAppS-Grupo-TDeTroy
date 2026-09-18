package com.tdetroy.valuacion.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import com.tdetroy.valuacion.model.Movimiento;
import com.tdetroy.valuacion.model.TipoMovimiento;
import com.tdetroy.valuacion.repositories.support.PostgresIntegrationTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * Test de integración de {@link MovimientoRepository} contra Postgres real (tasks.md T0.7,
 * constitution.md §1/§4). {@code MovimientoRepository} todavía no tiene métodos de consulta propios
 * (T5.5 los agrega cuando {@code MovimientoService} los necesite), así que este test cubre lo que
 * hoy expone: persistir un {@link Movimiento} válido vía {@link Movimiento#registrar} y leerlo de
 * vuelta con los mismos valores, y que la entidad es append-only también a nivel de persistencia
 * (dos movimientos guardados por separado quedan como dos filas independientes, sin que uno
 * sobrescriba al otro).
 */
class MovimientoRepositoryTest extends PostgresIntegrationTest {

    @Autowired private MovimientoRepository movimientoRepository;

    @Autowired private TestEntityManager entityManager;

    @Test
    void guardarMovimientoValido_persisteYSeLeeDeVueltaConLosValoresEsperados() {
        UUID usuarioId = UUID.randomUUID();
        UUID jugadorId = UUID.randomUUID();
        Instant fecha = Instant.parse("2026-01-15T10:00:00Z");
        Movimiento movimiento =
                Movimiento.registrar(
                        usuarioId,
                        jugadorId,
                        TipoMovimiento.COMPRA_SISTEMA,
                        5,
                        new BigDecimal("10.00"),
                        new BigDecimal("50.00"),
                        null,
                        fecha);

        Movimiento guardado = movimientoRepository.saveAndFlush(movimiento);
        entityManager.clear();

        Movimiento leido = movimientoRepository.findById(guardado.getId()).orElseThrow();

        assertThat(leido.getId()).isEqualTo(movimiento.getId());
        assertThat(leido.getUsuarioId()).isEqualTo(usuarioId);
        assertThat(leido.getJugadorId()).isEqualTo(jugadorId);
        assertThat(leido.getTipo()).isEqualTo(TipoMovimiento.COMPRA_SISTEMA);
        assertThat(leido.getCantidad()).isEqualTo(5);
        assertThat(leido.getPrecioUnitario()).isEqualByComparingTo("10.00");
        assertThat(leido.getMontoTotal()).isEqualByComparingTo("50.00");
        assertThat(leido.getContraparteUsuarioId()).isNull();
        assertThat(leido.getFecha()).isEqualTo(fecha);
    }

    @Test
    void dosMovimientosGuardadosPorSeparado_persistenComoFilasIndependientesSinSobrescribirse() {
        UUID usuarioId = UUID.randomUUID();
        Movimiento primero =
                Movimiento.registrar(
                        usuarioId,
                        null,
                        TipoMovimiento.RECARGA_SALDO,
                        null,
                        null,
                        new BigDecimal("100.00"),
                        null);
        Movimiento segundo =
                Movimiento.registrar(
                        usuarioId,
                        null,
                        TipoMovimiento.RECARGA_SALDO,
                        null,
                        null,
                        new BigDecimal("200.00"),
                        null);

        movimientoRepository.saveAndFlush(primero);
        movimientoRepository.saveAndFlush(segundo);
        entityManager.clear();

        assertThat(movimientoRepository.count()).isEqualTo(2);

        Movimiento primeroLeido = movimientoRepository.findById(primero.getId()).orElseThrow();
        Movimiento segundoLeido = movimientoRepository.findById(segundo.getId()).orElseThrow();

        assertThat(primeroLeido.getId()).isNotEqualTo(segundoLeido.getId());
        assertThat(primeroLeido.getMontoTotal()).isEqualByComparingTo("100.00");
        assertThat(segundoLeido.getMontoTotal()).isEqualByComparingTo("200.00");
    }
}
