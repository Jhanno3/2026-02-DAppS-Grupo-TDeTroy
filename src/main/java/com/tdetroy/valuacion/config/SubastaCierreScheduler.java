package com.tdetroy.valuacion.config;

import com.tdetroy.valuacion.services.SubastaService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dispara el cierre periódico de subastas (UC-18) invocando {@link SubastaService#cerrarVencidas}
 * directamente — sin Controller de por medio, mismo patrón que {@code OfertaVencimientoScheduler}.
 *
 * <p>Cada 10 segundos por defecto ({@code app.subastas.cierre-intervalo-ms}): la subasta se
 * adjudica a los 2 minutos sin pujas, así que el chequeo tiene que ser mucho más frecuente que el
 * de ofertas P2P.
 */
@Component
public class SubastaCierreScheduler {

    private final SubastaService subastaService;

    public SubastaCierreScheduler(SubastaService subastaService) {
        this.subastaService = subastaService;
    }

    @Scheduled(fixedDelayString = "${app.subastas.cierre-intervalo-ms:10000}")
    public void cerrarVencidas() {
        subastaService.cerrarVencidas();
    }
}
