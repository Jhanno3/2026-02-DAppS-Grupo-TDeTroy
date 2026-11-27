package com.tdetroy.valuacion.services;

import com.tdetroy.valuacion.services.jugador.RankingService;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.cache.annotation.CacheEvict;

/**
 * Marca una operación que modifica datos visibles en el ranking: vacía su caché al confirmarse.
 * Vive fuera de {@code jugador} porque la usan varios dominios.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@CacheEvict(cacheNames = RankingService.CACHE, allEntries = true)
public @interface InvalidaRanking {}
