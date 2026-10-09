package com.tdetroy.valuacion.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita el procesamiento de {@code @Scheduled} (tasks.md T3.6, plan.md §6.4/§8.2): sin esto, las
 * anotaciones {@code @Scheduled} de {@code config/} (ej. {@link RendimientoIngestaScheduler})
 * quedan declaradas pero nunca se disparan.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {}
