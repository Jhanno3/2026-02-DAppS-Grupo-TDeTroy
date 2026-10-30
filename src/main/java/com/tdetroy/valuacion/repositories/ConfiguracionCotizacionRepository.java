package com.tdetroy.valuacion.repositories;

import com.tdetroy.valuacion.entity.ConfiguracionCotizacionEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA sobre {@link ConfiguracionCotizacionEntity}: un único registro, con
 * id {@code ConfiguracionCotizacion.ID}.
 */
public interface ConfiguracionCotizacionRepository
        extends JpaRepository<ConfiguracionCotizacionEntity, UUID> {}
