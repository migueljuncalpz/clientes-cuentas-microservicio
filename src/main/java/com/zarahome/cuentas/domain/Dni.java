package com.zarahome.cuentas.domain;

import java.util.Locale;
import java.util.regex.Pattern;

public record Dni(String valor) {
    private static final Pattern FORMATO = Pattern.compile("[0-9]{8}[A-Z]");
    private static final String LETRAS_CONTROL = "TRWAGMYFPDXBNJZSQVHLCKE";

    public Dni {
        if (valor == null) {
            throw ErrorDominio.validacion("DNI_INVALIDO", "El DNI es obligatorio.");
        }
        valor = valor.toUpperCase(Locale.ROOT);
        if (!FORMATO.matcher(valor).matches()) {
            throw ErrorDominio.validacion("DNI_INVALIDO", "El DNI debe contener ocho dígitos y una letra.");
        }
        int numero = Integer.parseInt(valor.substring(0, 8));
        char letraEsperada = LETRAS_CONTROL.charAt(numero % LETRAS_CONTROL.length());
        if (valor.charAt(8) != letraEsperada) {
            throw ErrorDominio.validacion("DNI_INVALIDO", "La letra de control del DNI no es válida.");
        }
    }
}
