package com.zarahome.cuentas.infrastructure;

import com.zarahome.cuentas.application.port.in.GestionarCuentas;
import com.zarahome.cuentas.infrastructure.observability.CorrelationIdFilter;
import com.zarahome.cuentas.infrastructure.rest.CuentasController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CuentasController.class)
@Import(CorrelationIdFilter.class)
class ErroresRestTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GestionarCuentas cuentas;

    @Test
    void transformaUnaEscrituraObsoletaEnConflicto409() throws Exception {
        when(cuentas.actualizarSaldo(eq(1L), any()))
                .thenThrow(new OptimisticLockingFailureException("detalle de persistencia"));
        mvc.perform(put("/cuentas/1").with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_cuentas.write")))
                        .header("X-Correlation-ID", "conflicto-1")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"total\":1}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("CONFLICTO_CONCURRENCIA"))
                .andExpect(jsonPath("$.correlationId").value("conflicto-1"))
                .andExpect(header().string("X-Correlation-ID", "conflicto-1"));
    }

    @Test
    void ocultaDetallesDeBaseDeDatosEnLosConflictos() throws Exception {
        when(cuentas.actualizarSaldo(eq(1L), any()))
                .thenThrow(new DataIntegrityViolationException("SQL privado"));
        mvc.perform(put("/cuentas/1").with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_cuentas.write")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"total\":1}"))
                .andExpect(status().isConflict())
                .andExpect(content().string(not(containsString("SQL privado"))));
    }

    @Test
    void losErroresInesperadosDevuelvenUnProblemaSinDetallesInternos() throws Exception {
        when(cuentas.actualizarSaldo(eq(1L), any())).thenThrow(new IllegalStateException("detalle interno"));
        mvc.perform(put("/cuentas/1").with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_cuentas.write")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"total\":1}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"))
                .andExpect(content().string(not(containsString("detalle interno"))));
    }
}
