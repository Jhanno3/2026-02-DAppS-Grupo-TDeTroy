package com.tdetroy.valuacion.config;

import com.tdetroy.valuacion.model.OrigenCotizacion;
import com.tdetroy.valuacion.services.CotizacionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dispara el recalculo semanal de cotizacion (UC-06, tasks.md T4.5) invocando {@link
 * CotizacionService#recalcular} directamente -- nunca hay un Controller de por medio, el trigger no
 * es HTTP (constitution.md S2, mismo patron que {@code RendimientoIngestaScheduler}).
 *
 * <p>Lunes 03:00 (plan.md S6.4, S8.2): corre despues de la ventana de ingesta semanal (domingo
 * 01:00/02:00, {@code RendimientoIngestaScheduler}), con una hora de margen desde el fin de esa
 * ventana para que el calculo siempre disponga de los datos de rendimiento/resultados ya
 * actualizados de esa semana. {@code actorId = null} representa SISTEMA en el {@code
 * RegistroAuditoria} que escribe cada jugador afectado (ver {@code AuditoriaService}).
 */
@Component
public class CotizacionRecalculoScheduler {

    private final CotizacionService cotizacionService;

    public CotizacionRecalculoScheduler(CotizacionService cotizacionService) {
        this.cotizacionService = cotizacionService;
    }

    @Scheduled(cron = "${app.cotizacion.recalculo-cron:0 0 3 * * MON}")
    public void recalcular() {
        cotizacionService.recalcular(OrigenCotizacion.AUTOMATICO, null);
    }
}
