package com.zarahome.cuentas.application;

import com.zarahome.cuentas.application.port.in.ConsultarClientes;
import com.zarahome.cuentas.application.port.out.RepositorioClientes;
import com.zarahome.cuentas.application.port.out.Transacciones;
import com.zarahome.cuentas.domain.Cliente;
import com.zarahome.cuentas.domain.Dinero;
import com.zarahome.cuentas.domain.Dni;
import com.zarahome.cuentas.domain.ErrorDominio;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

public final class ServicioClientes implements ConsultarClientes {
    private final RepositorioClientes clientes;
    private final Transacciones transacciones;
    private final Clock reloj;

    public ServicioClientes(RepositorioClientes clientes, Transacciones transacciones, Clock reloj) {
        this.clientes = clientes;
        this.transacciones = transacciones;
        this.reloj = reloj;
    }

    @Override
    public List<Cliente> listar() {
        return transacciones.consultar(clientes::listarConCuentas);
    }

    @Override
    public List<Cliente> listarMayoresDeEdad() {
        LocalDate hoy = LocalDate.now(reloj);
        return transacciones.consultar(() -> clientes.listarConCuentas().stream()
                .filter(cliente -> cliente.esMayorDeEdad(hoy))
                .toList());
    }

    @Override
    public List<Cliente> listarConSaldoSuperiorA(Dinero cantidad) {
        return transacciones.consultar(() -> clientes.listarConCuentas().stream()
                .filter(cliente -> cliente.tieneSaldoSuperiorA(cantidad))
                .toList());
    }

    @Override
    public Cliente obtener(Dni dni) {
        return transacciones.consultar(() -> clientes.buscarConCuentas(dni)
                .orElseThrow(() -> ErrorDominio.noEncontrado("CLIENTE_NO_ENCONTRADO", "El cliente no existe.")));
    }
}
