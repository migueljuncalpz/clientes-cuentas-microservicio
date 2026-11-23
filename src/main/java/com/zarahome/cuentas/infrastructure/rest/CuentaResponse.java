package com.zarahome.cuentas.infrastructure.rest;

import com.zarahome.cuentas.domain.CuentaBancaria;
import com.zarahome.cuentas.domain.TipoCuenta;

import java.math.BigDecimal;

public record CuentaResponse(Long id, String dniCliente, TipoCuenta tipoCuenta, BigDecimal total) {
    public static CuentaResponse desde(CuentaBancaria cuenta) {
        return new CuentaResponse(cuenta.id(), cuenta.dniCliente().valor(), cuenta.tipoCuenta(), cuenta.total().valor());
    }
}
