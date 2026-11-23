package com.zarahome.cuentas.infrastructure.persistence;

import com.zarahome.cuentas.application.port.out.RepositorioClientes;
import com.zarahome.cuentas.domain.Cliente;
import com.zarahome.cuentas.domain.Dinero;
import com.zarahome.cuentas.domain.Dni;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class AdaptadorClientesJpa implements RepositorioClientes {
    private final ClientesJpaRepository clientes;
    private final EntityManager entityManager;

    public AdaptadorClientesJpa(ClientesJpaRepository clientes, EntityManager entityManager) {
        this.clientes = clientes;
        this.entityManager = entityManager;
    }

    @Override
    public List<Cliente> listarConCuentas() {
        return clientes.listarConCuentas().stream().map(MapeadorPersistencia::aDominio).toList();
    }

    @Override
    public List<Cliente> listarMayoresDeEdad(LocalDate fechaLimiteNacimiento) {
        return clientes.listarMayoresDeEdad(fechaLimiteNacimiento).stream()
                .map(MapeadorPersistencia::aDominio).toList();
    }

    @Override
    public List<Cliente> listarConSaldoSuperiorA(Dinero cantidad) {
        return clientes.listarConSaldoSuperiorA(cantidad.valor()).stream()
                .map(MapeadorPersistencia::aDominio).toList();
    }

    @Override
    public Optional<Cliente> buscarConCuentas(Dni dni) {
        return clientes.buscarConCuentas(dni.valor()).map(MapeadorPersistencia::aDominio);
    }

    @Override
    public boolean existe(Dni dni) {
        return clientes.existsById(dni.valor());
    }

    @Override
    public void crear(Cliente cliente) {
        entityManager.persist(new ClienteJpa(cliente.dni().valor(), cliente.nombre(), cliente.apellido1(),
                cliente.apellido2(), cliente.fechaNacimiento()));
    }
}
