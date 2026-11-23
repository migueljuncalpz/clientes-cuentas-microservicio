package com.zarahome.cuentas.infrastructure.rest;

import com.zarahome.cuentas.domain.TipoCuenta;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record CrearCuentaRequest(
        @NotBlank @Pattern(regexp = "[0-9]{8}[A-Za-z]") String dniCliente,
        @NotNull TipoCuenta tipoCuenta,
        @NotNull @DecimalMin("0") @DecimalMax("99999999999999999.99") BigDecimal total) {
}
