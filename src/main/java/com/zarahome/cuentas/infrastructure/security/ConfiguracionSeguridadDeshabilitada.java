package com.zarahome.cuentas.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@ConditionalOnProperty(name = "app.security.enabled", havingValue = "false")
class ConfiguracionSeguridadDeshabilitada {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfiguracionSeguridadDeshabilitada.class);

    @Bean
    SecurityFilterChain filtroSeguridadAbierto(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(autorizacion -> autorizacion.anyRequest().permitAll())
                .build();
    }

    @Bean
    ApplicationRunner advertirSeguridadDeshabilitada() {
        return argumentos -> LOGGER.warn("APP_SECURITY_ENABLED=false: autenticación y autorización desactivadas; API y Actuator quedan accesibles sin token");
    }
}
