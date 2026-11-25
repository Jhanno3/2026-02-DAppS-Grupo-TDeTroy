package com.tdetroy.valuacion.config.trazabilidad;

import static org.assertj.core.api.Assertions.assertThat;

import com.tdetroy.valuacion.common.CorrelationId;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

class CorrelationIdTareasConfigTest {

    private final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    private final Set<String> ids = ConcurrentHashMap.newKeySet();
    private final CountDownLatch dosEjecuciones = new CountDownLatch(2);

    @BeforeEach
    void setUp() {
        new CorrelationIdTareasConfig().correlationIdEnTareas().customize(scheduler);
        scheduler.initialize();
    }

    @AfterEach
    void tearDown() {
        scheduler.shutdown();
    }

    @Test
    void tareaConIntervalo_generaUnIdPorEjecucion() throws Exception {
        scheduler.scheduleWithFixedDelay(this::registrarId, Duration.ofMillis(10));

        verificarIdsDistintos();
    }

    @Test
    void tareaConCron_generaUnIdPorEjecucion() throws Exception {
        scheduler.schedule(this::registrarId, new CronTrigger("* * * * * *"));

        verificarIdsDistintos();
    }

    private void registrarId() {
        CorrelationId.actual().ifPresent(ids::add);
        dosEjecuciones.countDown();
    }

    private void verificarIdsDistintos() throws InterruptedException {
        assertThat(dosEjecuciones.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(ids).hasSizeGreaterThanOrEqualTo(2);
    }
}
