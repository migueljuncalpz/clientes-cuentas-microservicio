package com.zarahome.cuentas.domain;

import java.util.Locale;
import java.util.regex.Pattern;

public record Dni(String valor) {
    private static final Pattern FORMATO = Pattern.compile("[0-9]{8}[A-Z]");

    public Dni {
        if (valor == null) {
            throw ErrorDominio.validacion("DNI_INVALIDO", "El DNI es obligatorio.");
        }
        valor = valor.toUpperCase(Locale.ROOT);
        if (!FORMATO.matcher(valor).matches()) {
            throw ErrorDominio.validacion("DNI_INVALIDO", "El DNI debe contener ocho dígitos y una letra.");
        }
    }
}
