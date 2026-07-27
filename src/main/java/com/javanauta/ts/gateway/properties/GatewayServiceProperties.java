package com.javanauta.ts.gateway.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ts.services")
public record GatewayServiceProperties(
        String userServiceUri,
        String taskServiceUri
) {
}