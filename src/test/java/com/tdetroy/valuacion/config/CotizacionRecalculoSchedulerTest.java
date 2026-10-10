package com.tdetroy.valuacion.config;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.tdetroy.valuacion.model.OrigenCotizacion;
import com.tdetroy.valuacion.services.CotizacionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Cubre que {@link CotizacionRecalculoScheduler} sólo delega en {@link CotizacionService} con
 * {@code (AUTOMATICO, null)} (tasks.md T4.5) — el cron en sí (expresión, horario) no es
 * responsabilidad unitaria de este test, eso lo fija la configuración externa ({@code
 * application.properties}).
 */
@ExtendWith(MockitoExtension.class)
class CotizacionRecalculoSchedulerTest {

    @Mock private CotizacionService cotizacionService;

    @Test
    void recalcular_delegaEnElServicioConOrigenAutomaticoYActorNulo() {
        CotizacionRecalculoScheduler scheduler =
                new CotizacionRecalculoScheduler(cotizacionService);

        scheduler.recalcular();

        verify(cotizacionService).recalcular(OrigenCotizacion.AUTOMATICO, null);
        verifyNoMoreInteractions(cotizacionService);
    }
}
