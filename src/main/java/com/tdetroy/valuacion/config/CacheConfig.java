package com.tdetroy.valuacion.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.tdetroy.valuacion.services.jugador.RankingService;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Caché en memoria. El proxy transaccional posterga las invalidaciones hasta el commit: así una
 * consulta concurrente no vuelve a cachear los datos previos a una transacción todavía abierta. El
 * vencimiento acota cualquier desactualización residual.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final int MAXIMO_ENTRADAS = 100;

    @Bean
    CacheManager cacheManager(@Value("${app.ranking.cache-ttl:60s}") Duration ttl) {
        CaffeineCacheManager caffeine = new CaffeineCacheManager(RankingService.CACHE);
        caffeine.setCaffeine(
                Caffeine.newBuilder().maximumSize(MAXIMO_ENTRADAS).expireAfterWrite(ttl));
        return new TransactionAwareCacheManagerProxy(caffeine);
    }
}
