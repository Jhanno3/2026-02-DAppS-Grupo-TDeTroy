package com.tdetroy.valuacion.config;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.tdetroy.valuacion.services.RendimientoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Cubre que {@link RendimientoIngestaScheduler} sólo delega en {@link RendimientoService} (tasks.md
 * T3.6) — el cron en sí (expresión, horario) no es responsabilidad unitaria de este test, eso lo
 * fija la configuración externa ({@code application.properties}).
 */
@ExtendWith(MockitoExtension.class)
class RendimientoIngestaSchedulerTest {

    @Mock private RendimientoService rendimientoService;

    @Test
    void ingestarFixtures_delegaEnElServicio() {
        RendimientoIngestaScheduler scheduler = new RendimientoIngestaScheduler(rendimientoService);

        scheduler.ingestarFixtures();

        verify(rendimientoService).ingestarFixtures();
        verifyNoMoreInteractions(rendimientoService);
    }

    @Test
    void ingestarRendimiento_delegaEnElServicio() {
        RendimientoIngestaScheduler scheduler = new RendimientoIngestaScheduler(rendimientoService);

        scheduler.ingestarRendimiento();

        verify(rendimientoService).ingestarRendimiento();
        verifyNoMoreInteractions(rendimientoService);
    }
}
