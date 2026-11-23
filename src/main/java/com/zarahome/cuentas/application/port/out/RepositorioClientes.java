package com.zarahome.cuentas.application.port.out;

import com.zarahome.cuentas.domain.Cliente;
import com.zarahome.cuentas.domain.Dni;

import java.util.List;
import java.util.Optional;

public interface RepositorioClientes {
    List<Cliente> listarConCuentas();

    Optional<Cliente> buscarConCuentas(Dni dni);

    boolean existe(Dni dni);

    void crear(Cliente cliente);
}
