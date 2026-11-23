package com.zarahome.cuentas.infrastructure.config;

import com.zarahome.cuentas.application.ServicioClientes;
import com.zarahome.cuentas.application.ServicioCuentas;
import com.zarahome.cuentas.application.port.in.ConsultarClientes;
import com.zarahome.cuentas.application.port.in.GestionarCuentas;
import com.zarahome.cuentas.application.port.out.RepositorioClientes;
import com.zarahome.cuentas.application.port.out.RepositorioCuentas;
import com.zarahome.cuentas.application.port.out.Transacciones;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class ConfiguracionAplicacion {
    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }

    @Bean
    ConsultarClientes consultarClientes(RepositorioClientes clientes, Transacciones transacciones, Clock reloj) {
        return new ServicioClientes(clientes, transacciones, reloj);
    }

    @Bean
    GestionarCuentas gestionarCuentas(RepositorioClientes clientes, RepositorioCuentas cuentas,
                                    Transacciones transacciones) {
        return new ServicioCuentas(clientes, cuentas, transacciones);
    }
}
