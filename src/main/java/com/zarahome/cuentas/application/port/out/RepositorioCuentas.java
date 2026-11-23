package com.zarahome.cuentas.application.port.out;

import com.zarahome.cuentas.domain.CuentaBancaria;

import java.util.Optional;

public interface RepositorioCuentas {
    Optional<CuentaBancaria> buscar(long id);

    CuentaBancaria guardar(CuentaBancaria cuenta);
}
