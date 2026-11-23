package com.zarahome.cuentas.infrastructure;

import com.sun.net.httpserver.HttpServer;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ExportacionOtlpTest {
    @Test
    void exportaUnaTrazaPorHttpConLasDependenciasActualizadasDeKotlin() throws Exception {
        AtomicReference<byte[]> cuerpo = new AtomicReference<>();
        AtomicReference<String> tipoContenido = new AtomicReference<>();
        HttpServer colector = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        colector.createContext("/v1/traces", intercambio -> {
            try (intercambio) {
                cuerpo.set(intercambio.getRequestBody().readAllBytes());
                tipoContenido.set(intercambio.getRequestHeaders().getFirst("Content-Type"));
                intercambio.sendResponseHeaders(200, -1);
            }
        });
        colector.start();
        try (SdkTracerProvider proveedor = SdkTracerProvider.builder().build();
             OtlpHttpSpanExporter exportador = OtlpHttpSpanExporter.builder()
                     .setEndpoint("http://127.0.0.1:" + colector.getAddress().getPort() + "/v1/traces")
                     .setTimeout(Duration.ofSeconds(3))
                     .build()) {
            var span = proveedor.get("prueba-dependencias").spanBuilder("exportacion-otlp").startSpan();
            span.end();
            var resultado = exportador.export(List.of(((ReadableSpan) span).toSpanData()))
                    .join(5, TimeUnit.SECONDS);

            assertThat(resultado.isSuccess()).isTrue();
            assertThat(cuerpo.get()).isNotEmpty();
            assertThat(tipoContenido.get()).isEqualTo("application/x-protobuf");
        } finally {
            colector.stop(0);
        }
    }
}
