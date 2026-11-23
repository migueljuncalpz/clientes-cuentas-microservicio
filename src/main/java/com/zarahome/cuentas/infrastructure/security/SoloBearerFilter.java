package com.zarahome.cuentas.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/** Limits this API to Bearer authentication before Spring Security can process DPoP. */
final class SoloBearerFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean usaDPoP = request.getHeader("DPoP") != null
                || Collections.list(request.getHeaders(HttpHeaders.AUTHORIZATION)).stream()
                .anyMatch(valor -> valor.regionMatches(true, 0, "DPoP", 0, 4)
                        && (valor.length() == 4 || Character.isWhitespace(valor.charAt(4))));
        if (usaDPoP) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        chain.doFilter(request, response);
    }
}
