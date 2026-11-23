package com.zarahome.cuentas.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClientesJpaRepository extends JpaRepository<ClienteJpa, String> {
    @Query("select distinct c from ClienteJpa c left join fetch c.cuentas order by c.dni")
    List<ClienteJpa> listarConCuentas();

    @Query("select c from ClienteJpa c left join fetch c.cuentas where c.dni = :dni")
    Optional<ClienteJpa> buscarConCuentas(@Param("dni") String dni);
}
