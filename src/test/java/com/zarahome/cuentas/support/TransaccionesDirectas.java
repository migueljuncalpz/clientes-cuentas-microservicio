package com.zarahome.cuentas.support;

import com.zarahome.cuentas.application.port.out.Transacciones;

import java.util.function.Supplier;

public final class TransaccionesDirectas implements Transacciones {
    @Override
    public <T> T consultar(Supplier<T> operacion) {
        return operacion.get();
    }

    @Override
    public <T> T ejecutar(Supplier<T> operacion) {
        return operacion.get();
    }
}
