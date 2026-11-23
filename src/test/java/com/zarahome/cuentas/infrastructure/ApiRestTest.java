package com.zarahome.cuentas.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zarahome.cuentas.support.RelojPruebas;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.InputStream;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability
@ActiveProfiles("test")
@Import(RelojPruebas.class)
@SqlGroup({
        @Sql(statements = {"DELETE FROM cuentas_bancarias", "DELETE FROM clientes"}),
        @Sql(scripts = "/db/migration/V2__datos_iniciales.sql")
})
class ApiRestTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mapeos;

    @Test
    void elContratoTieneReferenciasValidasYCoincideConLosEndpointsImplementados() throws Exception {
        LoaderOptions opciones = new LoaderOptions();
        opciones.setAllowDuplicateKeys(false);
        JsonNode contrato;
        try (InputStream entrada = getClass().getResourceAsStream("/static/openapi.yaml")) {
            assertThat(entrada).isNotNull();
            Object documento = new Yaml(new SafeConstructor(opciones)).load(entrada);
            contrato = objectMapper.valueToTree(documento);
        }
        assertThat(contrato.path("openapi").asText()).isEqualTo("3.0.3");
        verificarReferencias(contrato, contrato);
        Set<String> declarados = new TreeSet<>();
        contrato.path("paths").properties().forEach(ruta ->
                ruta.getValue().fieldNames().forEachRemaining(metodo -> {
                    if (Set.of("get", "post", "put", "delete", "patch", "options", "head", "trace").contains(metodo)) {
                        declarados.add(metodo.toUpperCase(java.util.Locale.ROOT) + " " + ruta.getKey());
                    }
                }));
        Set<String> implementados = new TreeSet<>();
        mapeos.getHandlerMethods().forEach((mapeo, manejador) -> mapeo.getPatternValues().stream()
                .filter(ruta -> ruta.startsWith("/clientes") || ruta.startsWith("/cuentas"))
                .forEach(ruta -> mapeo.getMethodsCondition().getMethods()
                        .forEach(metodo -> implementados.add(metodo.name() + " " + ruta))));
        assertThat(implementados).containsExactlyInAnyOrderElementsOf(declarados);
    }

    private void verificarReferencias(JsonNode nodo, JsonNode raiz) {
        if (nodo.has("$ref")) {
            String referencia = nodo.get("$ref").asText();
            assertThat(referencia).startsWith("#/");
            assertThat(raiz.at(referencia.substring(1)).isMissingNode()).isFalse();
        }
        nodo.forEach(hijo -> verificarReferencias(hijo, raiz));
    }

    @Test
    void listaLosCincoClientesConSusSieteCuentasYSusDatosIniciales() throws Exception {
        mvc.perform(get("/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[*].dni", contains("11111111A", "22222222B", "33333333C", "44444444D", "55555555E")))
                .andExpect(jsonPath("$[0].nombre").value("Juan"))
                .andExpect(jsonPath("$[0].fechaNacimiento").value("1959-09-12"))
                .andExpect(jsonPath("$[0].cuentas", hasSize(2)))
                .andExpect(jsonPath("$[0].cuentas[0].total").value(150000.00))
                .andExpect(jsonPath("$[0].cuentas[1].total").value(20000.00))
                .andExpect(jsonPath("$[*].cuentas[*]", hasSize(7)));
    }

    @Test
    void devuelveSoloLosMayoresDeEdadConFechaDeterminista() throws Exception {
        mvc.perform(get("/clientes/mayores-de-edad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].dni", contains("11111111A", "22222222B", "44444444D", "55555555E")));
    }

    @Test
    void elFiltroSumaTodasLasCuentasYExcluyeLaIgualdad() throws Exception {
        mvc.perform(get("/clientes/con-cuenta-superior-a/150000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].dni", contains("11111111A")));
        mvc.perform(get("/clientes/con-cuenta-superior-a/170000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/clientes/con-cuenta-superior-a/0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));
    }

    @Test
    void consultaPorDniNormalizaLaLetraYDevuelveTodasLasCuentas() throws Exception {
        mvc.perform(get("/clientes/22222222b"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dni").value("22222222B"))
                .andExpect(jsonPath("$.nombre").value("Raúl"))
                .andExpect(jsonPath("$.cuentas", hasSize(2)));
    }

    @Test
    void informaDeUnClienteInexistenteConProblemaYCorrelacion() throws Exception {
        mvc.perform(get("/clientes/99999999Z").header("X-Correlation-ID", "prueba-404"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string("X-Correlation-ID", "prueba-404"))
                .andExpect(jsonPath("$.codigo").value("CLIENTE_NO_ENCONTRADO"))
                .andExpect(jsonPath("$.correlationId").value("prueba-404"))
                .andExpect(jsonPath("$.instance").value("/clientes/99999999Z"));
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void agregaUnaCuentaSinAlterarLosDatosDelClienteExistente() throws Exception {
        mvc.perform(post("/cuentas").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dniCliente":"11111111A","tipoCuenta":"NORMAL","total":50000.25}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.dniCliente").value("11111111A"))
                .andExpect(jsonPath("$.tipoCuenta").value("NORMAL"))
                .andExpect(jsonPath("$.total").value(50000.25));
        mvc.perform(get("/clientes/11111111A"))
                .andExpect(jsonPath("$.nombre").value("Juan"))
                .andExpect(jsonPath("$.fechaNacimiento").value("1959-09-12"))
                .andExpect(jsonPath("$.cuentas", hasSize(3)));
        mvc.perform(get("/clientes")).andExpect(jsonPath("$", hasSize(5)));
    }

    @Test
    void creaUnClienteConDatosPendientesYLoExcluyeDelFiltroDeEdad() throws Exception {
        mvc.perform(post("/cuentas").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dniCliente":"66666666f","tipoCuenta":"JUNIOR","total":0}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dniCliente").value("66666666F"));
        mvc.perform(get("/clientes/66666666F"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre", nullValue()))
                .andExpect(jsonPath("$.fechaNacimiento", nullValue()))
                .andExpect(jsonPath("$.cuentas", hasSize(1)));
        mvc.perform(get("/clientes")).andExpect(jsonPath("$", hasSize(6)));
        mvc.perform(get("/clientes/mayores-de-edad")).andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    void dosAltasRepresentanDosCuentasDistintas() throws Exception {
        String body = """
                {"dniCliente":"11111111A","tipoCuenta":"NORMAL","total":1}
                """;
        MvcResult primera = mvc.perform(post("/cuentas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        MvcResult segunda = mvc.perform(post("/cuentas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        JsonNode cuenta1 = objectMapper.readTree(primera.getResponse().getContentAsString());
        JsonNode cuenta2 = objectMapper.readTree(segunda.getResponse().getContentAsString());
        assertThat(cuenta1.get("id")).isNotEqualTo(cuenta2.get("id"));
        mvc.perform(get("/clientes/11111111A")).andExpect(jsonPath("$.cuentas", hasSize(4)));
    }

    @Test
    void reemplazarElSaldoEsIdempotenteYSeReflejaEnLaConsultaDelCliente() throws Exception {
        for (int intento = 0; intento < 2; intento++) {
            mvc.perform(put("/cuentas/1").contentType(MediaType.APPLICATION_JSON).content("{\"total\":180000.10}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.total").value(180000.10));
        }
        mvc.perform(get("/clientes/11111111A"))
                .andExpect(jsonPath("$.cuentas", hasSize(2)))
                .andExpect(jsonPath("$.cuentas[0].total").value(180000.10));
    }

    @Test
    void devuelve404AlActualizarUnaCuentaInexistente() throws Exception {
        mvc.perform(put("/cuentas/9999").contentType(MediaType.APPLICATION_JSON).content("{\"total\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("CUENTA_NO_ENCONTRADA"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}", "null", "{", "{\"total\":null}", "{\"total\":-1}", "{\"total\":0.001}",
            "{\"total\":100000000000000000}", "{\"total\":\"1\"}", "{\"total\":true}",
            "{\"total\":1,\"extra\":1}", "{\"total\":1} {\"total\":2}"
    })
    void rechazaActualizacionesInvalidasSinModificarElSaldo(String body) throws Exception {
        mvc.perform(put("/cuentas/1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(get("/clientes/11111111A")).andExpect(jsonPath("$.cuentas[0].total").value(150000.00));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"dniCliente\":\"mal\",\"tipoCuenta\":\"NORMAL\",\"total\":1}",
            "{\"dniCliente\":\"66666666F\",\"tipoCuenta\":\"INVALIDA\",\"total\":1}",
            "{\"dniCliente\":\"66666666F\",\"tipoCuenta\":0,\"total\":1}",
            "{\"dniCliente\":\"66666666F\",\"tipoCuenta\":null,\"total\":1}",
            "{\"dniCliente\":\"66666666F\",\"tipoCuenta\":\"NORMAL\",\"total\":-1}",
            "{\"dniCliente\":\"66666666F\",\"tipoCuenta\":\"NORMAL\",\"total\":0.001}"
    })
    void rechazaAltasInvalidasSinCrearClientesHuerfanos(String body) throws Exception {
        mvc.perform(post("/cuentas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/clientes")).andExpect(jsonPath("$", hasSize(5)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/clientes/invalido", "/clientes/con-cuenta-superior-a/abc",
            "/clientes/con-cuenta-superior-a/-1", "/clientes/con-cuenta-superior-a/0.001"})
    void rechazaParametrosDeConsultaInvalidos(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc"})
    void rechazaIdentificadoresDeCuentaInvalidos(String id) throws Exception {
        mvc.perform(put("/cuentas/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"total\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aceptaElSaldoMaximoSinRedondearlo() throws Exception {
        mvc.perform(put("/cuentas/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"total\":99999999999999999.99}"))
                .andExpect(status().isOk());
        MvcResult resultado = mvc.perform(get("/clientes/11111111A")).andExpect(status().isOk()).andReturn();
        JsonNode cliente = objectMapper.reader().with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .readTree(resultado.getResponse().getContentAsString());
        assertThat(cliente.path("cuentas").get(0).path("total").asText()).isEqualTo("99999999999999999.99");
    }

    @Test
    void sustituyeIdentificadoresDeCorrelacionInvalidosYLimpiaElMdc() throws Exception {
        MvcResult resultado = mvc.perform(get("/clientes").header("X-Correlation-ID", "no válido"))
                .andExpect(status().isOk()).andReturn();
        String generado = resultado.getResponse().getHeader("X-Correlation-ID");
        assertThat(UUID.fromString(generado)).isNotNull();
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void exponeElContratoLaSaludYLasMetricas() throws Exception {
        mvc.perform(get("/openapi.yaml")).andExpect(status().isOk())
                .andExpect(content().string(containsString("openapi: 3.0.3")));
        mvc.perform(get("/actuator/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/actuator/prometheus")).andExpect(status().isOk())
                .andExpect(content().string(containsString("jvm_memory_used_bytes")));
    }

    @Test
    void conservaElCodigoYLasCabecerasDeLosErroresHttp() throws Exception {
        mvc.perform(delete("/cuentas/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("PUT")))
                .andExpect(jsonPath("$.codigo").value("HTTP_405"));
        mvc.perform(get("/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("HTTP_404"));
    }
}
