package com.zarahome.cuentas.application.port.in;

import com.zarahome.cuentas.domain.Cliente;
import com.zarahome.cuentas.domain.Dinero;
import com.zarahome.cuentas.domain.Dni;

import java.util.List;

public interface ConsultarClientes {
    List<Cliente> listar();

    List<Cliente> listarMayoresDeEdad();

    List<Cliente> listarConSaldoSuperiorA(Dinero cantidad);

    Cliente obtener(Dni dni);
}
