package com.tdetroy.valuacion.services.jugador;

import com.tdetroy.valuacion.repositories.jugador.FilaRankingProjection;
import com.tdetroy.valuacion.repositories.jugador.JugadorRepository;
import java.util.List;
import org.springframework.stereotype.Component;

/** Estrategia de ranking por variación porcentual ("mayor suba", plan.md §7). */
@Component
public class EstrategiaRankingVariacion implements EstrategiaRanking {

    private final JugadorRepository jugadorRepository;

    public EstrategiaRankingVariacion(JugadorRepository jugadorRepository) {
        this.jugadorRepository = jugadorRepository;
    }

    @Override
    public String clave() {
        return "variacion-semanal";
    }

    @Override
    public List<FilaRankingProjection> buscarTop(int limite) {
        return jugadorRepository.buscarTopPorVariacion(limite);
    }
}
