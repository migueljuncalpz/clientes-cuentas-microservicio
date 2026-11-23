package com.zarahome.cuentas.application.port.in;

import com.zarahome.cuentas.domain.CuentaBancaria;
import com.zarahome.cuentas.domain.Dinero;
import com.zarahome.cuentas.domain.Dni;
import com.zarahome.cuentas.domain.TipoCuenta;

public interface GestionarCuentas {
    CuentaBancaria crear(Dni dniCliente, TipoCuenta tipoCuenta, Dinero total);

    CuentaBancaria actualizarSaldo(long idCuenta, Dinero total);
}
