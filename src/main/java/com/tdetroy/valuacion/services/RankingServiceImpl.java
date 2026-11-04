package com.tdetroy.valuacion.services;

import com.tdetroy.valuacion.model.JugadorConPrecio;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** {@inheritDoc} */
@Service
public class RankingServiceImpl implements RankingService {

    private static final int TOP_ELEMENTOS = 20;
    private static final String DEFAULT_ESTRATEGIA = "cotizacion";

    private final JugadorService jugadorService;
    private final Map<String, EstrategiaRanking> estrategias;

    public RankingServiceImpl(
            JugadorService jugadorService, List<EstrategiaRanking> estrategiasList) {
        this.jugadorService = jugadorService;
        this.estrategias = new HashMap<>();
        if (estrategiasList != null) {
            for (EstrategiaRanking estr : estrategiasList) {
                this.estrategias.put(estr.clave().toLowerCase(), estr);
            }
        }
    }

    @Override
    public List<JugadorConPrecio> obtenerRanking(String claveEstrategia) {
        String clave =
                (claveEstrategia != null && !claveEstrategia.isBlank())
                        ? claveEstrategia.trim().toLowerCase()
                        : DEFAULT_ESTRATEGIA;

        EstrategiaRanking estrategia =
                estrategias.getOrDefault(clave, estrategias.get(DEFAULT_ESTRATEGIA));
        String orden = estrategia != null ? estrategia.ordenMercado() : "precio-desc";

        return jugadorService.listarMercado(0, TOP_ELEMENTOS, orden, null, null).contenido();
    }
}
