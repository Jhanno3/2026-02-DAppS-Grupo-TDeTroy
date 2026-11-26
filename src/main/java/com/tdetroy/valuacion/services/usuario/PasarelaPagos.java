package com.tdetroy.valuacion.services.usuario;

import java.math.BigDecimal;

public interface PasarelaPagos {

    ResultadoPago procesarPago(BigDecimal monto, DatosTarjeta tarjeta);

    record DatosTarjeta(
            String numeroTarjeta, String titular, String vencimiento, String codigoSeguridad) {}

    record ResultadoPago(String codigoOperacion, String ultimosCuatroDigitos) {}
}
