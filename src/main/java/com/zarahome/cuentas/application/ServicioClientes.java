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
import java.time.Year;
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
        LocalDate fechaLimiteNacimiento = hoy.minusYears(18);
        if (hoy.getMonthValue() == 2 && hoy.getDayOfMonth() == 28 && Year.isLeap(hoy.getYear() - 18)) {
            fechaLimiteNacimiento = fechaLimiteNacimiento.plusDays(1);
        }
        LocalDate limite = fechaLimiteNacimiento;
        return transacciones.consultar(() -> clientes.listarMayoresDeEdad(limite));
    }

    @Override
    public List<Cliente> listarConSaldoSuperiorA(Dinero cantidad) {
        return transacciones.consultar(() -> clientes.listarConSaldoSuperiorA(cantidad));
    }

    @Override
    public Cliente obtener(Dni dni) {
        return transacciones.consultar(() -> clientes.buscarConCuentas(dni)
                .orElseThrow(() -> ErrorDominio.noEncontrado("CLIENTE_NO_ENCONTRADO", "El cliente no existe.")));
    }
}
