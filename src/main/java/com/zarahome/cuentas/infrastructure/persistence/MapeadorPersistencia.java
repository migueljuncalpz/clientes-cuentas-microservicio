package com.zarahome.cuentas.infrastructure.persistence;

import com.zarahome.cuentas.domain.Cliente;
import com.zarahome.cuentas.domain.CuentaBancaria;
import com.zarahome.cuentas.domain.Dinero;
import com.zarahome.cuentas.domain.Dni;

final class MapeadorPersistencia {
    private MapeadorPersistencia() {
    }

    static Cliente aDominio(ClienteJpa cliente) {
        return new Cliente(new Dni(cliente.getDni()), cliente.getNombre(), cliente.getApellido1(),
                cliente.getApellido2(), cliente.getFechaNacimiento(),
                cliente.getCuentas().stream().map(MapeadorPersistencia::aDominio).toList());
    }

    static CuentaBancaria aDominio(CuentaJpa cuenta) {
        return new CuentaBancaria(cuenta.getId(), new Dni(cuenta.getCliente().getDni()),
                cuenta.getTipoCuenta(), new Dinero(cuenta.getTotal()));
    }
}
