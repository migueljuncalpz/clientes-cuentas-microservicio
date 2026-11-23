package com.zarahome.cuentas.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ClientesJpaRepository extends JpaRepository<ClienteJpa, String> {
    @Query("select distinct c from ClienteJpa c left join fetch c.cuentas order by c.dni")
    List<ClienteJpa> listarConCuentas();

    @Query("select distinct c from ClienteJpa c left join fetch c.cuentas "
            + "where c.fechaNacimiento is not null and c.fechaNacimiento <= :fechaLimite order by c.dni")
    List<ClienteJpa> listarMayoresDeEdad(@Param("fechaLimite") LocalDate fechaLimiteNacimiento);

    @Query("select distinct c from ClienteJpa c left join fetch c.cuentas "
            + "where c.dni in (select titular.dni from ClienteJpa titular left join titular.cuentas cuenta "
            + "group by titular.dni having coalesce(sum(cuenta.total), 0) > :cantidad) order by c.dni")
    List<ClienteJpa> listarConSaldoSuperiorA(@Param("cantidad") BigDecimal cantidad);

    @Query("select c from ClienteJpa c left join fetch c.cuentas where c.dni = :dni")
    Optional<ClienteJpa> buscarConCuentas(@Param("dni") String dni);
}
