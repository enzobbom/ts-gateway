package com.javanauta.ts.gateway.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ts.jwt")
public record JwtProperties(
        String secret
) {
}
