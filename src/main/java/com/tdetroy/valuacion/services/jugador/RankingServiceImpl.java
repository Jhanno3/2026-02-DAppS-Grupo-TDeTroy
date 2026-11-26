package com.tdetroy.valuacion.services.jugador;

import com.tdetroy.valuacion.entity.jugador.JugadorEntity;
import com.tdetroy.valuacion.model.jugador.Jugador;
import com.tdetroy.valuacion.model.jugador.JugadorConPrecio;
import com.tdetroy.valuacion.repositories.jugador.FilaRankingProjection;
import com.tdetroy.valuacion.repositories.jugador.JugadorRepository;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/** {@inheritDoc} */
@Service
public class RankingServiceImpl implements RankingService {

    private static final int TOP_ELEMENTOS = 20;
    private static final String ESTRATEGIA_POR_DEFECTO = "cotizacion";

    private final JugadorRepository jugadorRepository;
    private final Map<String, EstrategiaRanking> estrategias;

    public RankingServiceImpl(
            JugadorRepository jugadorRepository, List<EstrategiaRanking> estrategiasDisponibles) {
        this.jugadorRepository = jugadorRepository;
        this.estrategias =
                estrategiasDisponibles.stream()
                        .collect(
                                Collectors.toUnmodifiableMap(
                                        EstrategiaRanking::clave, Function.identity()));
    }

    /**
     * {@inheritDoc}
     *
     * <p>{@code sync}: ante consultas simultáneas sin valor cacheado, sólo una va a la base.
     */
    @Override
    @Cacheable(cacheNames = CACHE, key = "#root.target.resolverClave(#p0)", sync = true)
    public List<JugadorConPrecio> obtenerRanking(String claveEstrategia) {
        List<FilaRankingProjection> filas =
                estrategias.get(resolverClave(claveEstrategia)).buscarTop(TOP_ELEMENTOS);

        Map<UUID, Jugador> jugadores =
                jugadorRepository
                        .findAllById(
                                filas.stream().map(FilaRankingProjection::getJugadorId).toList())
                        .stream()
                        .map(JugadorEntity::aModelo)
                        .collect(Collectors.toMap(Jugador::getId, Function.identity()));

        return filas.stream()
                .map(
                        fila ->
                                JugadorConPrecio.desdeCotizaciones(
                                        jugadores.get(fila.getJugadorId()),
                                        fila.getValorVigente(),
                                        fila.getFechaVigente(),
                                        fila.getValorInicial(),
                                        fila.getFechaInicial()))
                .toList();
    }

    /**
     * Clave registrada que corresponde a la pedida; la de por defecto si falta o no existe. También
     * es la clave de caché: así las variantes de un mismo pedido comparten entrada.
     */
    public String resolverClave(String claveEstrategia) {
        if (claveEstrategia == null) {
            return ESTRATEGIA_POR_DEFECTO;
        }
        String clave = claveEstrategia.trim().toLowerCase(Locale.ROOT);
        return estrategias.containsKey(clave) ? clave : ESTRATEGIA_POR_DEFECTO;
    }
}
