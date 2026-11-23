package com.zarahome.cuentas.infrastructure.rest;

import com.zarahome.cuentas.application.port.in.GestionarCuentas;
import com.zarahome.cuentas.domain.Dinero;
import com.zarahome.cuentas.domain.Dni;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cuentas")
public class CuentasController {
    private final GestionarCuentas cuentas;

    public CuentasController(GestionarCuentas cuentas) {
        this.cuentas = cuentas;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CuentaResponse crear(@Valid @RequestBody CrearCuentaRequest solicitud) {
        return CuentaResponse.desde(cuentas.crear(new Dni(solicitud.dniCliente()), solicitud.tipoCuenta(),
                new Dinero(solicitud.total())));
    }

    @PutMapping("/{idCuenta}")
    public CuentaResponse actualizarSaldo(@PathVariable long idCuenta,
                                         @Valid @RequestBody ActualizarSaldoRequest solicitud) {
        return CuentaResponse.desde(cuentas.actualizarSaldo(idCuenta, new Dinero(solicitud.total())));
    }
}
