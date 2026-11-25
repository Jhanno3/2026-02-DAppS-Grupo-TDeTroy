package com.tdetroy.valuacion.config.trazabilidad;

import com.tdetroy.valuacion.common.CorrelationId;
import org.slf4j.MDC;
import org.springframework.boot.task.ThreadPoolTaskSchedulerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cada ejecución de una tarea programada lleva su propio id. */
@Configuration
public class CorrelationIdTareasConfig {

    @Bean
    ThreadPoolTaskSchedulerCustomizer correlationIdEnTareas() {
        return scheduler -> scheduler.setTaskDecorator(CorrelationIdTareasConfig::conId);
    }

    private static Runnable conId(Runnable tarea) {
        return () -> {
            try (MDC.MDCCloseable ignorado =
                    MDC.putCloseable(CorrelationId.CLAVE_MDC, CorrelationId.generar())) {
                tarea.run();
            }
        };
    }
}
