package com.tdetroy.valuacion.services.jugador;

import org.springframework.stereotype.Component;

/** Estrategia de ranking por variación porcentual ("mayor suba", plan.md §7). */
@Component
public class EstrategiaRankingVariacion implements EstrategiaRanking {

    @Override
    public String clave() {
        return "variacion-semanal";
    }

    @Override
    public String ordenMercado() {
        return "suba";
    }
}
