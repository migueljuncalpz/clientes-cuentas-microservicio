package com.zarahome.cuentas.domain;

public final class ErrorDominio extends RuntimeException {
    public enum Tipo { VALIDACION, NO_ENCONTRADO }

    private final Tipo tipo;
    private final String codigo;

    private ErrorDominio(Tipo tipo, String codigo, String mensaje) {
        super(mensaje);
        this.tipo = tipo;
        this.codigo = codigo;
    }

    public static ErrorDominio validacion(String codigo, String mensaje) {
        return new ErrorDominio(Tipo.VALIDACION, codigo, mensaje);
    }

    public static ErrorDominio noEncontrado(String codigo, String mensaje) {
        return new ErrorDominio(Tipo.NO_ENCONTRADO, codigo, mensaje);
    }

    public Tipo tipo() {
        return tipo;
    }

    public String codigo() {
        return codigo;
    }
}
