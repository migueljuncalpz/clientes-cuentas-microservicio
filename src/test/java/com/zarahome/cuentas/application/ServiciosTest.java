package com.zarahome.cuentas.application;

import com.zarahome.cuentas.application.port.out.RepositorioClientes;
import com.zarahome.cuentas.application.port.out.RepositorioCuentas;
import com.zarahome.cuentas.application.port.out.Transacciones;
import com.zarahome.cuentas.domain.Cliente;
import com.zarahome.cuentas.domain.CuentaBancaria;
import com.zarahome.cuentas.domain.Dinero;
import com.zarahome.cuentas.domain.Dni;
import com.zarahome.cuentas.domain.ErrorDominio;
import com.zarahome.cuentas.domain.TipoCuenta;
import com.zarahome.cuentas.support.TransaccionesDirectas;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ServiciosTest {
    private final RepositorioClientes clientes = mock(RepositorioClientes.class);
    private final RepositorioCuentas cuentas = mock(RepositorioCuentas.class);
    private final TransaccionesDirectas transacciones = new TransaccionesDirectas();
    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-23T12:00:00Z"), ZoneOffset.UTC);
    private final ServicioClientes consultas = new ServicioClientes(clientes, transacciones, reloj);
    private final ServicioCuentas gestion = new ServicioCuentas(clientes, cuentas, transacciones);
    private final Dni dni = new Dni("11111111A");
    private final Dinero saldo = new Dinero(new BigDecimal("10.00"));

    @Test
    void distingueAdultosMenoresYFechasDesconocidasConRelojInyectado() {
        Cliente adulto = new Cliente(dni, "Juan", null, null, LocalDate.of(2008, 11, 23), List.of());
        LocalDate limite = LocalDate.of(2008, 11, 23);
        Cliente menor = new Cliente(new Dni("22222222B"), "Raúl", null, null, limite.plusDays(1), List.of());
        when(clientes.listarConCuentas()).thenReturn(List.of(adulto, menor, Cliente.pendiente(new Dni("33333333C"))));
        when(clientes.listarMayoresDeEdad(limite)).thenReturn(List.of(adulto));
        assertThat(consultas.listarMayoresDeEdad()).containsExactly(adulto);
        assertThat(consultas.listar()).hasSize(3);
        verify(clientes).listarMayoresDeEdad(limite);
    }

    @Test
    void filtraPorElSaldoAgregado() {
        Cliente cliente = new Cliente(dni, null, null, null, null,
                List.of(CuentaBancaria.nueva(dni, TipoCuenta.NORMAL, saldo)));
        Dinero cero = new Dinero(BigDecimal.ZERO);
        when(clientes.listarConSaldoSuperiorA(cero)).thenReturn(List.of(cliente));
        when(clientes.listarConSaldoSuperiorA(saldo)).thenReturn(List.of());
        assertThat(consultas.listarConSaldoSuperiorA(cero)).containsExactly(cliente);
        assertThat(consultas.listarConSaldoSuperiorA(saldo)).isEmpty();
        verify(clientes).listarConSaldoSuperiorA(cero);
    }

    @Test
    void incluyeElCumpleanosDelVeintinueveDeFebreroAlCumplirDieciocho() {
        Clock relojFebrero = Clock.fixed(Instant.parse("2026-02-28T12:00:00Z"), ZoneOffset.UTC);
        ServicioClientes consultasFebrero = new ServicioClientes(clientes, transacciones, relojFebrero);
        Cliente cliente = new Cliente(new Dni("12345678Z"), null, null, null, LocalDate.of(2008, 2, 29), List.of());
        when(clientes.listarMayoresDeEdad(LocalDate.of(2008, 2, 29))).thenReturn(List.of(cliente));

        assertThat(consultasFebrero.listarMayoresDeEdad()).containsExactly(cliente);
        verify(clientes).listarMayoresDeEdad(LocalDate.of(2008, 2, 29));
    }

    @Test
    void devuelveElClienteSolicitadoOUnErrorExplicito() {
        Cliente cliente = Cliente.pendiente(dni);
        when(clientes.buscarConCuentas(dni)).thenReturn(Optional.of(cliente));
        assertThat(consultas.obtener(dni)).isEqualTo(cliente);
        when(clientes.buscarConCuentas(dni)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> consultas.obtener(dni)).isInstanceOfSatisfying(ErrorDominio.class,
                error -> assertThat(error.codigo()).isEqualTo("CLIENTE_NO_ENCONTRADO"));
    }

    @Test
    void crearUnaCuentaDeUnClienteExistenteConservaSusDatos() {
        when(clientes.existe(dni)).thenReturn(true);
        CuentaBancaria guardada = new CuentaBancaria(8L, dni, TipoCuenta.NORMAL, saldo);
        when(cuentas.guardar(any())).thenReturn(guardada);
        assertThat(gestion.crear(dni, TipoCuenta.NORMAL, saldo)).isEqualTo(guardada);
        verify(clientes, never()).crear(any());
    }

    @Test
    void creaElClientePendienteCuandoNoExiste() {
        when(clientes.existe(dni)).thenReturn(false);
        when(cuentas.guardar(any())).thenReturn(new CuentaBancaria(8L, dni, TipoCuenta.NORMAL, saldo));
        gestion.crear(dni, TipoCuenta.NORMAL, saldo);
        verify(clientes).crear(Cliente.pendiente(dni));
    }

    @Test
    void reintentaEnUnaTransaccionNuevaSiElClienteSeCreoConcurrentemente() {
        AtomicInteger intentos = new AtomicInteger();
        Transacciones transaccionesConConflicto = new Transacciones() {
            @Override
            public <T> T consultar(Supplier<T> operacion) {
                return operacion.get();
            }

            @Override
            public <T> T ejecutar(Supplier<T> operacion) {
                if (intentos.incrementAndGet() == 1) {
                    operacion.get();
                    throw new ConflictoPersistenciaException(new RuntimeException("clave duplicada al confirmar"));
                }
                return operacion.get();
            }
        };
        ServicioCuentas servicio = new ServicioCuentas(clientes, cuentas, transaccionesConConflicto);
        CuentaBancaria guardada = new CuentaBancaria(8L, dni, TipoCuenta.NORMAL, saldo);
        when(clientes.existe(dni)).thenReturn(false, true, true);
        when(cuentas.guardar(any())).thenReturn(guardada);

        assertThat(servicio.crear(dni, TipoCuenta.NORMAL, saldo)).isEqualTo(guardada);
        verify(clientes).crear(Cliente.pendiente(dni));
        verify(cuentas, org.mockito.Mockito.times(2)).guardar(any());
        assertThat(intentos).hasValue(2);
    }

    @Test
    void actualizaElSaldoOInformaDeLaCuentaInexistente() {
        CuentaBancaria cuenta = new CuentaBancaria(1L, dni, TipoCuenta.NORMAL, saldo);
        when(cuentas.buscar(1)).thenReturn(Optional.of(cuenta));
        when(cuentas.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        Dinero nuevoSaldo = new Dinero(new BigDecimal("20.00"));
        assertThat(gestion.actualizarSaldo(1, nuevoSaldo).total()).isEqualTo(nuevoSaldo);
        assertThatThrownBy(() -> gestion.actualizarSaldo(999, saldo)).isInstanceOfSatisfying(ErrorDominio.class,
                error -> assertThat(error.codigo()).isEqualTo("CUENTA_NO_ENCONTRADA"));
    }

    @Test
    void rechazaUnIdentificadorInvalidoAntesDeConsultarLaPersistencia() {
        assertThatThrownBy(() -> gestion.actualizarSaldo(0, saldo)).isInstanceOf(ErrorDominio.class);
        verifyNoInteractions(clientes, cuentas);
    }
}
