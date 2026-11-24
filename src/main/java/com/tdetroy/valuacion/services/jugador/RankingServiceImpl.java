package com.tdetroy.valuacion.services.jugador;

import com.tdetroy.valuacion.model.jugador.JugadorConPrecio;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** {@inheritDoc} */
@Service
public class RankingServiceImpl implements RankingService {

    private static final int TOP_ELEMENTOS = 20;
    private static final String ESTRATEGIA_POR_DEFECTO = "cotizacion";

    private final JugadorService jugadorService;
    private final Map<String, EstrategiaRanking> estrategias;

    public RankingServiceImpl(
            JugadorService jugadorService, List<EstrategiaRanking> estrategiasDisponibles) {
        this.jugadorService = jugadorService;
        this.estrategias = new HashMap<>();
        if (estrategiasDisponibles != null) {
            for (EstrategiaRanking estr : estrategiasDisponibles) {
                this.estrategias.put(estr.clave().toLowerCase(), estr);
            }
        }
    }

    @Override
    public List<JugadorConPrecio> obtenerRanking(String claveEstrategia) {
        String clave =
                (claveEstrategia != null && !claveEstrategia.isBlank())
                        ? claveEstrategia.trim().toLowerCase()
                        : ESTRATEGIA_POR_DEFECTO;

        EstrategiaRanking estrategia =
                estrategias.getOrDefault(clave, estrategias.get(ESTRATEGIA_POR_DEFECTO));
        String orden = estrategia != null ? estrategia.ordenMercado() : "precio-desc";

        return jugadorService.listarMercado(0, TOP_ELEMENTOS, orden, null, null).contenido();
    }
}
