package com.tdetroy.valuacion.services.jugador;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.tdetroy.valuacion.config.CacheConfig;
import com.tdetroy.valuacion.repositories.jugador.JugadorRepository;
import com.tdetroy.valuacion.services.InvalidaRanking;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

/** Comportamiento de la caché del ranking, con los proxies de Spring pero sin base de datos. */
class RankingCacheTest {

    private final JugadorRepository jugadorRepository = mock(JugadorRepository.class);
    private AnnotationConfigApplicationContext contexto;
    private RankingService ranking;

    @BeforeEach
    void setUp() {
        contexto = new AnnotationConfigApplicationContext();
        contexto.getBeanFactory()
                .setConversionService(ApplicationConversionService.getSharedInstance());
        contexto.registerBean(JugadorRepository.class, () -> jugadorRepository);
        contexto.register(
                CacheConfig.class,
                RankingServiceImpl.class,
                EstrategiaRankingCotizacion.class,
                EstrategiaRankingVariacion.class,
                Modificador.class);
        contexto.refresh();
        ranking = contexto.getBean(RankingService.class);
    }

    @AfterEach
    void tearDown() {
        contexto.close();
    }

    @Test
    void lasVariantesDeUnaMismaEstrategiaCompartenEntrada() {
        ranking.obtenerRanking("cotizacion");
        ranking.obtenerRanking(" COTIZACION ");
        ranking.obtenerRanking("inexistente");
        ranking.obtenerRanking(null);
        ranking.obtenerRanking("variacion-semanal");
        ranking.obtenerRanking("variacion-semanal");

        verify(jugadorRepository, times(1)).buscarTopPorCotizacion(20);
        verify(jugadorRepository, times(1)).buscarTopPorVariacion(20);
    }

    @Test
    void laInvalidacionEsperaAlCommit() {
        ranking.obtenerRanking("cotizacion");

        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            contexto.getBean(Operacion.class).modificar();
            ranking.obtenerRanking("cotizacion");
            verify(jugadorRepository, times(1)).buscarTopPorCotizacion(20);
            TransactionSynchronizationUtils.triggerAfterCommit();
        } finally {
            TransactionSynchronizationManager.clear();
        }

        ranking.obtenerRanking("cotizacion");
        verify(jugadorRepository, times(2)).buscarTopPorCotizacion(20);
    }

    static class Modificador implements Operacion {
        @Override
        @InvalidaRanking
        public void modificar() {
            // Sólo dispara la invalidación.
        }
    }

    interface Operacion {
        void modificar();
    }
}
