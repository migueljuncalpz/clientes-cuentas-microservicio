package com.zarahome.cuentas.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Arrays;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability
@ActiveProfiles("test")
class SeguridadResourceServerTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void permiteLaDocumentacionPeroProtegeApiYActuator() throws Exception {
        mvc.perform(get("/clientes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/openapi.yaml")).andExpect(status().isOk());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isUnauthorized());
    }

    @Test
    void rechazaBearerInvalido() throws Exception {
        mvc.perform(get("/clientes").header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aplicaPermisosDeLecturaYEscrituraDeCuentas() throws Exception {
        mvc.perform(get("/clientes").with(jwtConPermisos("cuentas.read")))
                .andExpect(status().isOk());
        mvc.perform(post("/cuentas").with(jwtConPermisos("cuentas.read"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dniCliente\":\"11111111H\",\"tipoCuenta\":\"NORMAL\",\"total\":1}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/cuentas").with(jwtConPermisos("cuentas.write"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dniCliente\":\"11111111H\",\"tipoCuenta\":\"NORMAL\",\"total\":1}"))
                .andExpect(status().isCreated());
    }

    @Test
    void separaPermisosDeSaludYMetricas() throws Exception {
        mvc.perform(get("/actuator/health").with(jwtConPermisos("actuator.health.read")))
                .andExpect(status().isOk());
        mvc.perform(get("/actuator/health").with(jwtConPermisos("actuator.metrics.read")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/actuator/prometheus").with(jwtConPermisos("actuator.metrics.read")))
                .andExpect(status().isOk());
        mvc.perform(get("/actuator/prometheus").with(jwtConPermisos("actuator.health.read")))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"DPoP token", "dpop token", "DPoP", "DPoP\ttoken"})
    void rechazaDPoPAntesDeProcesarLaPrueba(String autorizacion) throws Exception {
        mvc.perform(get("/clientes").header("Authorization", autorizacion)
                        .with(jwtConPermisos("cuentas.read")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
    }

    @Test
    void rechazaPruebasDPoPAunqueElBearerSeaValido() throws Exception {
        mvc.perform(get("/clientes").header("DPoP", "prueba-no-admitida")
                        .with(jwtConPermisos("cuentas.read")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rechazaDPoPEnCualquieraDeLasCabecerasDeAutorizacion() throws Exception {
        mvc.perform(get("/clientes").header("Authorization", "Bearer token", "DPoP token")
                        .with(jwtConPermisos("cuentas.read")))
                .andExpect(status().isUnauthorized());
    }

    private RequestPostProcessor jwtConPermisos(String... permisos) {
        return jwt().authorities(Arrays.stream(permisos)
                .map(permiso -> new SimpleGrantedAuthority("SCOPE_" + permiso))
                .toArray(GrantedAuthority[]::new));
    }
}
