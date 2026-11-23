package com.zarahome.cuentas.infrastructure.persistence;

import com.zarahome.cuentas.application.port.out.RepositorioCuentas;
import com.zarahome.cuentas.domain.CuentaBancaria;
import com.zarahome.cuentas.domain.ErrorDominio;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AdaptadorCuentasJpa implements RepositorioCuentas {
    private final CuentasJpaRepository cuentas;
    private final ClientesJpaRepository clientes;

    public AdaptadorCuentasJpa(CuentasJpaRepository cuentas, ClientesJpaRepository clientes) {
        this.cuentas = cuentas;
        this.clientes = clientes;
    }

    @Override
    public Optional<CuentaBancaria> buscar(long id) {
        return cuentas.findById(id).map(MapeadorPersistencia::aDominio);
    }

    @Override
    public CuentaBancaria guardar(CuentaBancaria cuenta) {
        if (cuenta.id() == null) {
            ClienteJpa titular = clientes.getReferenceById(cuenta.dniCliente().valor());
            return MapeadorPersistencia.aDominio(cuentas.save(new CuentaJpa(titular, cuenta.tipoCuenta(),
                    cuenta.total().valor())));
        }
        CuentaJpa existente = cuentas.findById(cuenta.id())
                .orElseThrow(() -> ErrorDominio.noEncontrado("CUENTA_NO_ENCONTRADA", "La cuenta no existe."));
        existente.actualizarSaldo(cuenta.total().valor());
        return MapeadorPersistencia.aDominio(existente);
    }
}
