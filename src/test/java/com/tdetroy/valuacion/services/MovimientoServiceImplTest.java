package com.tdetroy.valuacion.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tdetroy.valuacion.entity.MovimientoEntity;
import com.tdetroy.valuacion.model.Movimiento;
import com.tdetroy.valuacion.model.TipoMovimiento;
import com.tdetroy.valuacion.repositories.MovimientoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Cubre {@link MovimientoServiceImpl} (tasks.md T5.5, UC-12): historial cronológico de un usuario,
 * delegado íntegramente en {@code MovimientoRepository} — sin reclasificar ni filtrar ningún {@code
 * TipoMovimiento} (compensaciones y P2P quedan identificadas por su propio tipo).
 */
@ExtendWith(MockitoExtension.class)
class MovimientoServiceImplTest {

    @Mock private MovimientoRepository movimientoRepository;

    private MovimientoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MovimientoServiceImpl(movimientoRepository);
    }

    @Test
    void obtenerHistorial_conMovimientosPrevios_devuelveElHistorialCronologicoCompleto() {
        UUID usuarioId = UUID.randomUUID();
        Movimiento recarga =
                Movimiento.registrar(
                        usuarioId,
                        null,
                        TipoMovimiento.RECARGA_SALDO,
                        null,
                        null,
                        new BigDecimal("100.00"),
                        null,
                        Instant.parse("2026-01-01T00:00:00Z"));
        Movimiento compensacion =
                Movimiento.registrar(
                        usuarioId,
                        UUID.randomUUID(),
                        TipoMovimiento.COMPENSACION_BAJA,
                        3,
                        new BigDecimal("20.00"),
                        new BigDecimal("60.00"),
                        null,
                        Instant.parse("2026-02-01T00:00:00Z"));
        when(movimientoRepository.findByUsuarioIdOrderByFechaAsc(usuarioId))
                .thenReturn(
                        List.of(
                                MovimientoEntity.desde(recarga),
                                MovimientoEntity.desde(compensacion)));

        List<Movimiento> historial = service.obtenerHistorial(usuarioId);

        assertThat(historial)
                .extracting(Movimiento::getTipo)
                .containsExactly(TipoMovimiento.RECARGA_SALDO, TipoMovimiento.COMPENSACION_BAJA);
        assertThat(historial.get(1).getCantidad()).isEqualTo(3);
    }

    @Test
    void obtenerHistorial_sinMovimientos_devuelveListaVacia() {
        UUID usuarioId = UUID.randomUUID();
        when(movimientoRepository.findByUsuarioIdOrderByFechaAsc(usuarioId)).thenReturn(List.of());

        assertThat(service.obtenerHistorial(usuarioId)).isEmpty();
    }
}
