package com.zarahome.cuentas.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReglasDominioTest {
    private static final Dni DNI = new Dni("11111111H");

    @Test
    void normalizaElDniYValidaSuLetraDeControl() {
        assertThat(new Dni("11111111h")).isEqualTo(DNI);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "1111111H", "111111111", " 11111111H", "11111111HH", "11111111A"})
    void rechazaDnisInvalidos(String valor) {
        assertThatThrownBy(() -> new Dni(valor)).isInstanceOf(ErrorDominio.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "0.001", "100000000000000000.00"})
    void rechazaImportesQueNoPuedenPersistirseSinPerdida(String valor) {
        assertThatThrownBy(() -> dinero(valor)).isInstanceOf(ErrorDominio.class);
    }

    @Test
    void rechazaImportesNulosYConservaExactitudDecimal() {
        assertThatThrownBy(() -> new Dinero(null)).isInstanceOf(ErrorDominio.class);
        assertThat(dinero("0.10").valor().add(dinero("0.20").valor())).isEqualByComparingTo("0.30");
        assertThat(dinero("99999999999999999.99").valor()).hasScaleOf(2);
        assertThat(dinero("0").valor()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void laMayoriaDeEdadComienzaElDiaDelDecimoctavoCumpleanos() {
        Cliente cliente = cliente(LocalDate.of(2008, 11, 23), List.of());
        assertThat(cliente.esMayorDeEdad(LocalDate.of(2026, 11, 22))).isFalse();
        assertThat(cliente.esMayorDeEdad(LocalDate.of(2026, 11, 23))).isTrue();
        assertThat(cliente.esMayorDeEdad(LocalDate.of(2026, 11, 24))).isTrue();
        assertThat(Cliente.pendiente(DNI).esMayorDeEdad(LocalDate.of(2026, 11, 23))).isFalse();
    }

    @Test
    void comparaLaSumaDeTodasLasCuentasConUnLimiteEstricto() {
        Cliente cliente = cliente(null, List.of(cuenta(1L, "150000"), cuenta(2L, "20000")));
        assertThat(cliente.tieneSaldoSuperiorA(dinero("150000"))).isTrue();
        assertThat(cliente.tieneSaldoSuperiorA(dinero("170000"))).isFalse();
        assertThat(Cliente.pendiente(DNI).tieneSaldoSuperiorA(dinero("0"))).isFalse();
    }

    @Test
    void elSaldoAgregadoPuedeSuperarElLimiteDeUnaCuentaIndividual() {
        Cliente cliente = cliente(null, List.of(cuenta(1L, "99999999999999999.99"), cuenta(2L, "0.01")));
        assertThat(cliente.tieneSaldoSuperiorA(dinero("99999999999999999.99"))).isTrue();
    }

    @Test
    void protegeLaColeccionYRechazaCuentasDeOtroTitular() {
        List<CuentaBancaria> cuentas = new ArrayList<>();
        Cliente cliente = cliente(null, cuentas);
        cuentas.add(cuenta(1L, "10"));
        assertThat(cliente.cuentas()).isEmpty();
        assertThatThrownBy(() -> cliente.cuentas().add(cuenta(2L, "10")))
                .isInstanceOf(UnsupportedOperationException.class);
        CuentaBancaria ajena = CuentaBancaria.nueva(new Dni("22222222J"), TipoCuenta.NORMAL, dinero("1"));
        assertThatThrownBy(() -> cliente(null, List.of(ajena))).isInstanceOf(ErrorDominio.class);
    }

    @Test
    void actualizarUnSaldoNoMutaLaCuentaOriginal() {
        CuentaBancaria original = cuenta(1L, "10.10");
        CuentaBancaria actualizada = original.actualizarSaldo(dinero("20.20"));
        assertThat(original.total()).isEqualTo(dinero("10.10"));
        assertThat(actualizada.total()).isEqualTo(dinero("20.20"));
        assertThat(actualizada.id()).isEqualTo(original.id());
        assertThatThrownBy(() -> cuenta(0L, "10")).isInstanceOf(ErrorDominio.class);
        assertThatThrownBy(() -> cuenta(-1L, "10")).isInstanceOf(ErrorDominio.class);
    }

    private static Dinero dinero(String valor) {
        return new Dinero(new BigDecimal(valor));
    }

    private static CuentaBancaria cuenta(Long id, String saldo) {
        return new CuentaBancaria(id, DNI, TipoCuenta.NORMAL, dinero(saldo));
    }

    private static Cliente cliente(LocalDate nacimiento, List<CuentaBancaria> cuentas) {
        return new Cliente(DNI, "Juan", "Pérez", "López", nacimiento, cuentas);
    }
}
