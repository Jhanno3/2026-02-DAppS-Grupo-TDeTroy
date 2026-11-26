package com.tdetroy.valuacion.services.jugador;

import com.tdetroy.valuacion.repositories.jugador.FilaRankingProjection;
import com.tdetroy.valuacion.repositories.jugador.JugadorRepository;
import java.util.List;
import org.springframework.stereotype.Component;

/** Estrategia de ranking por defecto: ordena por cotización vigente descendente (plan.md §7). */
@Component
public class EstrategiaRankingCotizacion implements EstrategiaRanking {

    private final JugadorRepository jugadorRepository;

    public EstrategiaRankingCotizacion(JugadorRepository jugadorRepository) {
        this.jugadorRepository = jugadorRepository;
    }

    @Override
    public String clave() {
        return "cotizacion";
    }

    @Override
    public List<FilaRankingProjection> buscarTop(int limite) {
        return jugadorRepository.buscarTopPorCotizacion(limite);
    }
}
