package com.zarahome.cuentas.infrastructure.persistence;

import com.zarahome.cuentas.domain.TipoCuenta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "cuentas_bancarias")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CuentaJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dni_cliente", nullable = false, updatable = false)
    private ClienteJpa cliente;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cuenta", nullable = false, length = 16, updatable = false)
    private TipoCuenta tipoCuenta;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @Version
    @Column(nullable = false)
    private Long version;

    public CuentaJpa(ClienteJpa cliente, TipoCuenta tipoCuenta, BigDecimal total) {
        this.cliente = cliente;
        this.tipoCuenta = tipoCuenta;
        this.total = total;
    }

    public void actualizarSaldo(BigDecimal nuevoSaldo) {
        total = nuevoSaldo;
    }
}
