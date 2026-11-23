package com.zarahome.cuentas.infrastructure.rest;

import com.zarahome.cuentas.domain.Cliente;

import java.time.LocalDate;
import java.util.List;

public record ClienteResponse(String dni, String nombre, String apellido1, String apellido2,
                              LocalDate fechaNacimiento, List<CuentaResponse> cuentas) {
    public static ClienteResponse desde(Cliente cliente) {
        return new ClienteResponse(cliente.dni().valor(), cliente.nombre(), cliente.apellido1(),
                cliente.apellido2(), cliente.fechaNacimiento(),
                cliente.cuentas().stream().map(CuentaResponse::desde).toList());
    }
}
