package com.tdetroy.valuacion.services;

import com.tdetroy.valuacion.entity.MovimientoEntity;
import com.tdetroy.valuacion.model.Movimiento;
import com.tdetroy.valuacion.repositories.MovimientoRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** {@inheritDoc} */
@Service
public class MovimientoServiceImpl implements MovimientoService {

    private final MovimientoRepository movimientoRepository;

    public MovimientoServiceImpl(MovimientoRepository movimientoRepository) {
        this.movimientoRepository = movimientoRepository;
    }

    @Override
    public List<Movimiento> obtenerHistorial(UUID usuarioId) {
        return movimientoRepository.findByUsuarioIdOrderByFechaAsc(usuarioId).stream()
                .map(MovimientoEntity::aModelo)
                .toList();
    }
}
