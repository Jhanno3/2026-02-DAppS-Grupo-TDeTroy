-- Estrategia de cotizacion por defecto elegida por un ADMIN en runtime. Registro unico (id fijo,
-- ConfiguracionCotizacion.ID): sin fila, vale app.cotizacion.estrategia-default.
CREATE TABLE configuracion_cotizacion (
    id                  UUID PRIMARY KEY,
    estrategia_default  VARCHAR(30) NOT NULL,
    fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL
);
