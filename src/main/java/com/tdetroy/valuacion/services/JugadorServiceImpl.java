package com.tdetroy.valuacion.services;

import com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException;
import com.tdetroy.valuacion.model.Jugador;
import com.tdetroy.valuacion.repositories.JugadorRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** {@inheritDoc} */
@Service
public class JugadorServiceImpl implements JugadorService {

    private static final String ENTIDAD_AUDITADA = "Jugador";

    private final JugadorRepository jugadorRepository;
    private final AuditoriaService auditoriaService;

    public JugadorServiceImpl(
            JugadorRepository jugadorRepository, AuditoriaService auditoriaService) {
        this.jugadorRepository = jugadorRepository;
        this.auditoriaService = auditoriaService;
    }

    @Override
    public List<Jugador> listar() {
        return jugadorRepository.findAll();
    }

    @Override
    public Jugador obtenerPorId(UUID jugadorId) {
        return jugadorRepository
                .findById(jugadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException(ENTIDAD_AUDITADA, jugadorId));
    }

    @Override
    @Transactional
    public Jugador darAlta(
            String nombre,
            String club,
            String posicion,
            LocalDate fechaNacimiento,
            String nacionalidad,
            UUID actorId) {
        Jugador jugador =
                jugadorRepository.save(
                        Jugador.darAlta(nombre, club, posicion, fechaNacimiento, nacionalidad));

        auditoriaService.registrar(
                actorId,
                "ALTA_JUGADOR",
                ENTIDAD_AUDITADA,
                jugador.getId(),
                null,
                datosIdentificatoriosDe(jugador));

        return jugador;
    }

    @Override
    @Transactional
    public Jugador editar(
            UUID jugadorId,
            String nombre,
            String club,
            String posicion,
            LocalDate fechaNacimiento,
            String nacionalidad,
            UUID actorId) {
        Jugador jugador =
                jugadorRepository
                        .findById(jugadorId)
                        .orElseThrow(
                                () ->
                                        new RecursoNoEncontradoException(
                                                ENTIDAD_AUDITADA, jugadorId));

        DatosIdentificatoriosAuditoria antes = datosIdentificatoriosDe(jugador);
        jugador.actualizarDatosIdentificatorios(
                nombre, club, posicion, fechaNacimiento, nacionalidad);

        auditoriaService.registrar(
                actorId,
                "EDICION_JUGADOR",
                ENTIDAD_AUDITADA,
                jugadorId,
                antes,
                datosIdentificatoriosDe(jugador));

        return jugador;
    }

    private static DatosIdentificatoriosAuditoria datosIdentificatoriosDe(Jugador jugador) {
        return new DatosIdentificatoriosAuditoria(
                jugador.getNombre(),
                jugador.getClub(),
                jugador.getPosicion(),
                jugador.getFechaNacimiento(),
                jugador.getNacionalidad());
    }

    private record DatosIdentificatoriosAuditoria(
            String nombre,
            String club,
            String posicion,
            LocalDate fechaNacimiento,
            String nacionalidad) {}
}
