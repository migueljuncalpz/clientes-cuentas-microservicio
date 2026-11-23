package com.zarahome.cuentas.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Dinero(BigDecimal valor) {
    private static final BigDecimal MAXIMO = new BigDecimal("99999999999999999.99");

    public Dinero {
        if (valor == null || valor.signum() < 0 || valor.compareTo(MAXIMO) > 0) {
            throw ErrorDominio.validacion("IMPORTE_INVALIDO", "El importe debe estar entre 0 y 99999999999999999.99.");
        }
        try {
            valor = valor.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw ErrorDominio.validacion("IMPORTE_INVALIDO", "El importe admite como máximo dos decimales.");
        }
    }
}
