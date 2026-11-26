package com.tdetroy.valuacion.services.cartera;

import com.tdetroy.valuacion.entity.cartera.MovimientoEntity;
import com.tdetroy.valuacion.model.cartera.Movimiento;
import com.tdetroy.valuacion.repositories.cartera.MovimientoRepository;
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
