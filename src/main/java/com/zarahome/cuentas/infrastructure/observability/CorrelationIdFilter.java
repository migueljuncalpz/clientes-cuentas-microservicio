package com.zarahome.cuentas.infrastructure.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class CorrelationIdFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Correlation-ID";
    private static final Pattern FORMATO = Pattern.compile("[A-Za-z0-9._-]{1,64}");
    private static final Logger LOG = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String recibido = request.getHeader(HEADER);
        String correlationId = recibido != null && FORMATO.matcher(recibido).matches()
                ? recibido : UUID.randomUUID().toString();
        response.setHeader(HEADER, correlationId);
        try (MDC.MDCCloseable contexto = MDC.putCloseable("correlationId", correlationId)) {
            try {
                chain.doFilter(request, response);
            } finally {
                LOG.info("Petición HTTP completada: método={}, estado={}", request.getMethod(), response.getStatus());
            }
        }
    }
}
