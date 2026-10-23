package com.tdetroy.valuacion.config;

import com.tdetroy.valuacion.services.OfertaService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dispara el chequeo periódico de ofertas P2P vencidas (UC-17, "si pasa esa cantidad de horas sin
 * venderse, se cancela la oferta p2p y los tokens vuelven al usuario que la creó") invocando {@link
 * OfertaService#cancelarVencidas} directamente — nunca hay un Controller de por medio, el trigger
 * no es HTTP (constitution.md §2, mismo patrón que {@code CotizacionRecalculoScheduler}).
 *
 * <p>Cada 5 minutos por defecto (configurable, {@code app.ofertas.vencimiento-cron}): a diferencia
 * del recálculo semanal de cotización, una oferta vence en horas, no semanas, así que necesita un
 * chequeo mucho más frecuente para que el vencimiento sea razonablemente puntual.
 */
@Component
public class OfertaVencimientoScheduler {

    private final OfertaService ofertaService;

    public OfertaVencimientoScheduler(OfertaService ofertaService) {
        this.ofertaService = ofertaService;
    }

    @Scheduled(cron = "${app.ofertas.vencimiento-cron:0 */5 * * * *}")
    public void cancelarVencidas() {
        ofertaService.cancelarVencidas();
    }
}
