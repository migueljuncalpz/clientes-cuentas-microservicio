package com.zarahome.cuentas.application;

public final class ConflictoPersistenciaException extends RuntimeException {
    public ConflictoPersistenciaException(Throwable causa) {
        super("La operación entra en conflicto con otra escritura.", causa);
    }
}
