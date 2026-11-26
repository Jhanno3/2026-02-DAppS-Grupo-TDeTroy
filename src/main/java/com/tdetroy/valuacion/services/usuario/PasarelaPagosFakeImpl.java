package com.tdetroy.valuacion.services.usuario;

import com.tdetroy.valuacion.common.exceptions.PagoRechazadoException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PasarelaPagosFakeImpl implements PasarelaPagos {

    private static final DateTimeFormatter FORMATO_VENCIMIENTO =
            DateTimeFormatter.ofPattern("MM/yy");

    @Override
    public ResultadoPago procesarPago(BigDecimal monto, DatosTarjeta tarjeta) {
        if (monto == null || monto.signum() <= 0) {
            throw new PagoRechazadoException("El monto a cargar debe ser mayor a cero.");
        }
        if (tarjeta == null
                || tarjeta.numeroTarjeta() == null
                || !tarjeta.numeroTarjeta().matches("^\\d{16}$")) {
            throw new PagoRechazadoException("El número de tarjeta debe tener 16 dígitos.");
        }
        if (tarjeta.titular() == null || tarjeta.titular().isBlank()) {
            throw new PagoRechazadoException("El titular de la tarjeta es obligatorio.");
        }
        if (tarjeta.codigoSeguridad() == null || !tarjeta.codigoSeguridad().matches("^\\d{3,4}$")) {
            throw new PagoRechazadoException("El código de seguridad debe tener 3 o 4 dígitos.");
        }
        validarVencimiento(tarjeta.vencimiento());

        String ultimosCuatro = tarjeta.numeroTarjeta().substring(12);
        String codigoOperacion =
                "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        return new ResultadoPago(codigoOperacion, ultimosCuatro);
    }

    private void validarVencimiento(String vencimiento) {
        if (vencimiento == null || !vencimiento.matches("^(0[1-9]|1[0-2])/\\d{2}$")) {
            throw new PagoRechazadoException("El vencimiento debe tener formato MM/AA.");
        }
        try {
            YearMonth fechaVencimiento = YearMonth.parse(vencimiento, FORMATO_VENCIMIENTO);
            if (fechaVencimiento.isBefore(YearMonth.now())) {
                throw new PagoRechazadoException("La tarjeta ingresada se encuentra vencida.");
            }
        } catch (DateTimeParseException ex) {
            throw new PagoRechazadoException("El vencimiento debe tener formato MM/AA.");
        }
    }
}
