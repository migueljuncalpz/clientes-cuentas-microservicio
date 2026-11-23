package com.zarahome.cuentas.application.port.out;

import com.zarahome.cuentas.domain.Cliente;
import com.zarahome.cuentas.domain.Dni;
import com.zarahome.cuentas.domain.Dinero;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RepositorioClientes {
    List<Cliente> listarConCuentas();

    List<Cliente> listarMayoresDeEdad(LocalDate fechaLimiteNacimiento);

    List<Cliente> listarConSaldoSuperiorA(Dinero cantidad);

    Optional<Cliente> buscarConCuentas(Dni dni);

    boolean existe(Dni dni);

    void crear(Cliente cliente);
}
