package com.zarahome.cuentas.infrastructure.security;

import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPublicKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

@Configuration
public class ConfiguracionSeguridad {
    private static final String CUENTAS_READ = "SCOPE_cuentas.read";
    private static final String CUENTAS_WRITE = "SCOPE_cuentas.write";
    private static final String ACTUATOR_HEALTH_READ = "SCOPE_actuator.health.read";
    private static final String ACTUATOR_METRICS_READ = "SCOPE_actuator.metrics.read";

    @Bean
    SecurityFilterChain filtroSeguridad(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(autorizacion -> autorizacion
                        .requestMatchers(HttpMethod.GET, "/clientes", "/clientes/**").hasAuthority(CUENTAS_READ)
                        .requestMatchers(HttpMethod.POST, "/cuentas").hasAuthority(CUENTAS_WRITE)
                        .requestMatchers(HttpMethod.PUT, "/cuentas/**").hasAuthority(CUENTAS_WRITE)
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**")
                        .hasAuthority(ACTUATOR_HEALTH_READ)
                        .requestMatchers(HttpMethod.GET, "/actuator/prometheus").hasAuthority(ACTUATOR_METRICS_READ)
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()))
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(ResourceLoader resourceLoader,
                          @Value("${app.security.jwt.issuer-uri}") String issuerUri,
                          @Value("${app.security.jwt.jwk-set-uri:}") String jwkSetUri,
                          @Value("${app.security.jwt.public-key-location:}") String publicKeyLocation,
                          @Value("${app.security.jwt.audience}") String audience,
                          @Value("${app.security.jwt.jws-algorithm:RS256}") String jwsAlgorithm) throws IOException {
        boolean tieneJwks = StringUtils.hasText(jwkSetUri);
        boolean tieneClavePublica = StringUtils.hasText(publicKeyLocation);
        if (tieneJwks == tieneClavePublica) {
            throw new IllegalStateException("Configura exactamente uno entre JWT_JWKS_URI y LOCAL_JWT_PUBLIC_KEY");
        }
        if (!StringUtils.hasText(issuerUri) || !StringUtils.hasText(audience)) {
            throw new IllegalStateException("JWT_ISSUER_URI y JWT_AUDIENCE son obligatorios");
        }

        SignatureAlgorithm algoritmo = SignatureAlgorithm.valueOf(jwsAlgorithm);
        NimbusJwtDecoder decoder = tieneJwks
                ? NimbusJwtDecoder.withJwkSetUri(jwkSetUri).jwsAlgorithm(algoritmo).build()
                : decoderConClavePublica(resourceLoader, publicKeyLocation, algoritmo);
        OAuth2TokenValidator<Jwt> validadorAudiencia = jwt -> jwt.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "JWT audience is invalid", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuerUri), validadorAudiencia));
        return decoder;
    }

    private NimbusJwtDecoder decoderConClavePublica(ResourceLoader resourceLoader, String ubicacion,
                                                     SignatureAlgorithm algoritmo) throws IOException {
        Resource recurso = resourceLoader.getResource(ubicacion);
        try (InputStream entrada = recurso.getInputStream()) {
            RSAPublicKey clave = RsaKeyConverters.x509().convert(entrada);
            if (clave == null) {
                throw new IllegalStateException("No se pudo leer la clave pública RSA local");
            }
            return NimbusJwtDecoder.withPublicKey(clave).signatureAlgorithm(algoritmo).build();
        }
    }
}
