package com.javanauta.ts.gateway.security.principal;

import org.springframework.web.servlet.function.ServerRequest;

public interface AuthenticatedPrincipalRequestCustomizer {
    void customize(ServerRequest request, AuthenticatedPrincipal principal);
}