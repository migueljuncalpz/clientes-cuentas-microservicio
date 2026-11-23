package com.zarahome.cuentas.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CuentasJpaRepository extends JpaRepository<CuentaJpa, Long> {
}
