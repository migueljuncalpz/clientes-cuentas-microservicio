package com.zarahome.cuentas.infrastructure.rest;

import com.zarahome.cuentas.application.port.in.ConsultarClientes;
import com.zarahome.cuentas.domain.Dinero;
import com.zarahome.cuentas.domain.Dni;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/clientes")
public class ClientesController {
    private final ConsultarClientes clientes;

    public ClientesController(ConsultarClientes clientes) {
        this.clientes = clientes;
    }

    @GetMapping
    public List<ClienteResponse> listar() {
        return clientes.listar().stream().map(ClienteResponse::desde).toList();
    }

    @GetMapping("/mayores-de-edad")
    public List<ClienteResponse> listarMayoresDeEdad() {
        return clientes.listarMayoresDeEdad().stream().map(ClienteResponse::desde).toList();
    }

    @GetMapping("/con-cuenta-superior-a/{cantidad}")
    public List<ClienteResponse> listarConSaldoSuperiorA(@PathVariable BigDecimal cantidad) {
        return clientes.listarConSaldoSuperiorA(new Dinero(cantidad)).stream()
                .map(ClienteResponse::desde).toList();
    }

    @GetMapping("/{dni}")
    public ClienteResponse obtener(@PathVariable String dni) {
        return ClienteResponse.desde(clientes.obtener(new Dni(dni)));
    }
}
