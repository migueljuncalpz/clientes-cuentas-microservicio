package com.zarahome.cuentas.application.port.out;

import java.util.function.Supplier;

public interface Transacciones {
    <T> T consultar(Supplier<T> operacion);

    <T> T ejecutar(Supplier<T> operacion);
}
