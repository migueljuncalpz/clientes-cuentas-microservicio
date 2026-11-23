package com.zarahome.cuentas.application;

import com.zarahome.cuentas.application.port.in.GestionarCuentas;
import com.zarahome.cuentas.application.port.out.RepositorioClientes;
import com.zarahome.cuentas.application.port.out.RepositorioCuentas;
import com.zarahome.cuentas.application.port.out.Transacciones;
import com.zarahome.cuentas.domain.Cliente;
import com.zarahome.cuentas.domain.CuentaBancaria;
import com.zarahome.cuentas.domain.Dinero;
import com.zarahome.cuentas.domain.Dni;
import com.zarahome.cuentas.domain.ErrorDominio;
import com.zarahome.cuentas.domain.TipoCuenta;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ServicioCuentas implements GestionarCuentas {
    private final RepositorioClientes clientes;
    private final RepositorioCuentas cuentas;
    private final Transacciones transacciones;

    public ServicioCuentas(RepositorioClientes clientes, RepositorioCuentas cuentas, Transacciones transacciones) {
        this.clientes = clientes;
        this.cuentas = cuentas;
        this.transacciones = transacciones;
    }

    @Override
    public CuentaBancaria crear(Dni dniCliente, TipoCuenta tipoCuenta, Dinero total) {
        CuentaBancaria nuevaCuenta = CuentaBancaria.nueva(dniCliente, tipoCuenta, total);
        AtomicBoolean clienteNuevo = new AtomicBoolean();
        try {
            return transacciones.ejecutar(() -> {
                if (!clientes.existe(dniCliente)) {
                    clienteNuevo.set(true);
                    clientes.crear(Cliente.pendiente(dniCliente));
                }
                return cuentas.guardar(nuevaCuenta);
            });
        } catch (ConflictoPersistenciaException conflicto) {
            if (!clienteNuevo.get() || !transacciones.consultar(() -> clientes.existe(dniCliente))) {
                throw conflicto;
            }
            return transacciones.ejecutar(() -> cuentas.guardar(nuevaCuenta));
        }
    }

    @Override
    public CuentaBancaria actualizarSaldo(long idCuenta, Dinero total) {
        if (idCuenta <= 0) {
            throw ErrorDominio.validacion("ID_CUENTA_INVALIDO", "El identificador de cuenta debe ser positivo.");
        }
        Objects.requireNonNull(total, "El saldo es obligatorio.");
        return transacciones.ejecutar(() -> cuentas.buscar(idCuenta)
                .map(cuenta -> cuentas.guardar(cuenta.actualizarSaldo(total)))
                .orElseThrow(() -> ErrorDominio.noEncontrado("CUENTA_NO_ENCONTRADA", "La cuenta no existe.")));
    }
}
