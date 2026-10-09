package com.tdetroy.valuacion.config;

import com.tdetroy.valuacion.services.RendimientoService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dispara la ingesta semanal de rendimiento (UC-05, tasks.md T3.6) invocando {@link
 * RendimientoService} directamente — nunca hay un Controller de por medio, ninguno de los dos
 * disparadores es una request HTTP (constitution.md §1: "el scraping de WhoScored no se ejecuta en
 * el flujo síncrono de un request de usuario").
 *
 * <p>Domingo 01:00 fixtures, domingo 02:00 rendimiento (plan.md §8.2): el margen de una hora deja
 * que Football-Data.org confirme qué partidos ya se disputaron antes de que WhoScored (T3.3)
 * scrapee su detalle, y ambos terminan con margen antes del job de recotización del lunes 03:00
 * (T4.5). Los cron quedan externalizados con el valor del plan como default — la cadencia semanal
 * en sí no es ajustable sin reabrir la constitución (plan.md §8.2), pero el minuto exacto dentro de
 * esa ventana sí puede correrse por operación sin tocar código.
 */
@Component
public class RendimientoIngestaScheduler {

    private final RendimientoService rendimientoService;

    public RendimientoIngestaScheduler(RendimientoService rendimientoService) {
        this.rendimientoService = rendimientoService;
    }

    @Scheduled(cron = "${app.rendimiento.ingesta.fixtures-cron:0 0 1 * * SUN}")
    public void ingestarFixtures() {
        rendimientoService.ingestarFixtures();
    }

    @Scheduled(cron = "${app.rendimiento.ingesta.rendimiento-cron:0 0 2 * * SUN}")
    public void ingestarRendimiento() {
        rendimientoService.ingestarRendimiento();
    }
}
