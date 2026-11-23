package com.zarahome.cuentas.domain;

import java.util.Objects;

public record CuentaBancaria(Long id, Dni dniCliente, TipoCuenta tipoCuenta, Dinero total) {
    public CuentaBancaria {
        if (id != null && id <= 0) {
            throw ErrorDominio.validacion("ID_CUENTA_INVALIDO", "El identificador de cuenta debe ser positivo.");
        }
        Objects.requireNonNull(dniCliente, "El titular es obligatorio.");
        Objects.requireNonNull(tipoCuenta, "El tipo de cuenta es obligatorio.");
        Objects.requireNonNull(total, "El saldo es obligatorio.");
    }

    public static CuentaBancaria nueva(Dni dniCliente, TipoCuenta tipoCuenta, Dinero total) {
        return new CuentaBancaria(null, dniCliente, tipoCuenta, total);
    }

    public CuentaBancaria actualizarSaldo(Dinero nuevoSaldo) {
        return new CuentaBancaria(id, dniCliente, tipoCuenta, nuevoSaldo);
    }
}
