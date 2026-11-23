package com.zarahome.cuentas.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record Cliente(Dni dni, String nombre, String apellido1, String apellido2,
                      LocalDate fechaNacimiento, List<CuentaBancaria> cuentas) {
    public Cliente {
        Objects.requireNonNull(dni, "El DNI es obligatorio.");
        cuentas = List.copyOf(cuentas);
        if (cuentas.stream().anyMatch(cuenta -> !cuenta.dniCliente().equals(dni))) {
            throw ErrorDominio.validacion("TITULAR_INCORRECTO", "Todas las cuentas deben pertenecer al cliente.");
        }
    }

    public static Cliente pendiente(Dni dni) {
        return new Cliente(dni, null, null, null, null, List.of());
    }

    public boolean esMayorDeEdad(LocalDate fechaActual) {
        return fechaNacimiento != null && !fechaNacimiento.plusYears(18).isAfter(fechaActual);
    }

    public boolean tieneSaldoSuperiorA(Dinero cantidad) {
        return cuentas.stream()
                .map(cuenta -> cuenta.total().valor())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .compareTo(cantidad.valor()) > 0;
    }
}
