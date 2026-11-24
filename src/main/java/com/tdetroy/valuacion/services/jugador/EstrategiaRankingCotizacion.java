package com.tdetroy.valuacion.services.jugador;

import org.springframework.stereotype.Component;

/** Estrategia de ranking por defecto: ordena por cotización vigente descendente (plan.md §7). */
@Component
public class EstrategiaRankingCotizacion implements EstrategiaRanking {

    @Override
    public String clave() {
        return "cotizacion";
    }

    @Override
    public String ordenMercado() {
        return "precio-desc";
    }
}
