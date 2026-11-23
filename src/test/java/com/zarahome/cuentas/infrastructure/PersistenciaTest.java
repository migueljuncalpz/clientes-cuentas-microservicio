package com.zarahome.cuentas.infrastructure;

import com.zarahome.cuentas.application.port.out.RepositorioClientes;
import com.zarahome.cuentas.application.port.out.RepositorioCuentas;
import com.zarahome.cuentas.application.port.out.Transacciones;
import com.zarahome.cuentas.domain.Cliente;
import com.zarahome.cuentas.domain.CuentaBancaria;
import com.zarahome.cuentas.domain.Dinero;
import com.zarahome.cuentas.domain.Dni;
import com.zarahome.cuentas.domain.TipoCuenta;
import com.zarahome.cuentas.support.RelojPruebas;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability
@ActiveProfiles("test")
@Import(RelojPruebas.class)
@SqlGroup({
        @Sql(statements = {"DELETE FROM cuentas_bancarias", "DELETE FROM clientes"}),
        @Sql(scripts = "/db/migration/V2__datos_iniciales.sql")
})
class PersistenciaTest {
    @Autowired
    private Transacciones transacciones;

    @Autowired
    private RepositorioClientes clientes;

    @Autowired
    private RepositorioCuentas cuentas;

    @Test
    void revierteClienteYCuentaSiFallaLaTransaccion() {
        Dni dni = new Dni("66666666F");
        assertThatThrownBy(() -> transacciones.ejecutar(() -> {
            clientes.crear(Cliente.pendiente(dni));
            cuentas.guardar(CuentaBancaria.nueva(dni, TipoCuenta.NORMAL, new Dinero(BigDecimal.TEN)));
            throw new IllegalStateException("Fallo deliberado después de insertar la cuenta");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(transacciones.consultar(() -> clientes.existe(dni))).isFalse();
        assertThat(transacciones.consultar(clientes::listarConCuentas).stream()
                .flatMap(cliente -> cliente.cuentas().stream()).count()).isEqualTo(7);
    }

    @Test
    void detectaDosActualizacionesConcurrentesSinPerderUnaEscrituraSilenciosamente() throws Exception {
        CountDownLatch lecturasCompletadas = new CountDownLatch(2);
        int confirmadas = 0;
        int conflictos = 0;
        try (ExecutorService ejecutor = Executors.newFixedThreadPool(2)) {
            List<Future<CuentaBancaria>> resultados = List.of(
                    ejecutor.submit(() -> actualizarTrasLecturaCompartida("10", lecturasCompletadas)),
                    ejecutor.submit(() -> actualizarTrasLecturaCompartida("20", lecturasCompletadas)));
            for (Future<CuentaBancaria> resultado : resultados) {
                try {
                    resultado.get(15, TimeUnit.SECONDS);
                    confirmadas++;
                } catch (ExecutionException exception) {
                    assertThat(exception.getCause()).isInstanceOf(OptimisticLockingFailureException.class);
                    conflictos++;
                }
            }
        }
        assertThat(confirmadas).isEqualTo(1);
        assertThat(conflictos).isEqualTo(1);
        BigDecimal saldo = transacciones.consultar(() -> cuentas.buscar(1).orElseThrow().total().valor());
        assertThat(saldo).isIn(new BigDecimal("10.00"), new BigDecimal("20.00"));
    }

    private CuentaBancaria actualizarTrasLecturaCompartida(String saldo, CountDownLatch lecturasCompletadas) {
        return transacciones.ejecutar(() -> {
            CuentaBancaria cuenta = cuentas.buscar(1).orElseThrow();
            lecturasCompletadas.countDown();
            esperar(lecturasCompletadas);
            return cuentas.guardar(cuenta.actualizarSaldo(new Dinero(new BigDecimal(saldo))));
        });
    }

    private void esperar(CountDownLatch lecturasCompletadas) {
        try {
            if (!lecturasCompletadas.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("No se completaron ambas lecturas concurrentes");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Prueba concurrente interrumpida", exception);
        }
    }
}
