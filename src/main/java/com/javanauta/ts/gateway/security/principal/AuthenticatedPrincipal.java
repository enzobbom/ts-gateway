package com.javanauta.ts.gateway.security.principal;

import java.util.UUID;

public record AuthenticatedPrincipal(
        UUID userId,
        String email
) {
}