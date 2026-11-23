package com.zarahome.cuentas.infrastructure.rest;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ActualizarSaldoRequest(
        @NotNull @DecimalMin("0") @DecimalMax("99999999999999999.99") BigDecimal total) {
}
