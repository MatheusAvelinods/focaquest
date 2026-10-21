package com.focaquest.auth;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param jwtSecret    chave HMAC (mínimo 32 bytes) usada para assinar os tokens
 * @param tokenTtl     validade do token de sessão
 * @param secureCookie define o atributo Secure do cookie (true em produção, com HTTPS)
 */
@ConfigurationProperties(prefix = "focaquest.auth")
public record AuthProperties(String jwtSecret, Duration tokenTtl, boolean secureCookie) {

    public AuthProperties {
        if (jwtSecret == null || jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("focaquest.auth.jwt-secret deve ter pelo menos 32 bytes");
        }
        if (tokenTtl == null) {
            tokenTtl = Duration.ofDays(7);
        }
    }
}
