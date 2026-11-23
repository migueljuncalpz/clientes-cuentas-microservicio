package com.zarahome.cuentas.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clientes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClienteJpa {
    @Id
    @Column(length = 9, nullable = false, updatable = false)
    private String dni;

    private String nombre;
    private String apellido1;
    private String apellido2;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @OneToMany(mappedBy = "cliente")
    @OrderBy("id ASC")
    private List<CuentaJpa> cuentas = new ArrayList<>();

    public ClienteJpa(String dni, String nombre, String apellido1, String apellido2, LocalDate fechaNacimiento) {
        this.dni = dni;
        this.nombre = nombre;
        this.apellido1 = apellido1;
        this.apellido2 = apellido2;
        this.fechaNacimiento = fechaNacimiento;
    }
}
