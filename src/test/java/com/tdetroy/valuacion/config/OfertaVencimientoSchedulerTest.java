package com.tdetroy.valuacion.config;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.tdetroy.valuacion.services.OfertaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Cubre que {@link OfertaVencimientoScheduler} sólo delega en {@link
 * OfertaService#cancelarVencidas} (UC-17) — el cron en sí (expresión, cadencia) no es
 * responsabilidad unitaria de este test, eso lo fija la configuración externa ({@code
 * application.properties}).
 */
@ExtendWith(MockitoExtension.class)
class OfertaVencimientoSchedulerTest {

    @Mock private OfertaService ofertaService;

    @Test
    void cancelarVencidas_delegaEnElServicio() {
        OfertaVencimientoScheduler scheduler = new OfertaVencimientoScheduler(ofertaService);

        scheduler.cancelarVencidas();

        verify(ofertaService).cancelarVencidas();
        verifyNoMoreInteractions(ofertaService);
    }
}
