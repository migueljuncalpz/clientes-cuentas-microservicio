package com.zarahome.cuentas.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.security.enabled=false")
@AutoConfigureMockMvc
@AutoConfigureObservability
@ActiveProfiles("test")
class SeguridadDeshabilitadaTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void permiteAccederSinTokenATodosLosEndpointsAlDeshabilitarSeguridad() throws Exception {
        mvc.perform(get("/clientes")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/actuator/prometheus")).andExpect(status().isOk());
        mvc.perform(post("/cuentas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dniCliente\":\"11111111A\",\"tipoCuenta\":\"NORMAL\",\"total\":1}"))
                .andExpect(status().isCreated());
    }
}
