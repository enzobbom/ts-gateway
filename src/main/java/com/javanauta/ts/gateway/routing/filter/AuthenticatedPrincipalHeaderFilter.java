package com.javanauta.ts.gateway.routing.filter;

import com.javanauta.ts.apicontract.http.HttpHeaders;
import com.javanauta.ts.gateway.security.principal.AuthenticatedPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

@Component
public class AuthenticatedPrincipalHeaderFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    @Override
    public ServerResponse filter(ServerRequest request, HandlerFunction<ServerResponse> next) throws Exception {
        HttpServletRequest servletRequest = request.servletRequest();

        AuthenticatedPrincipal principal = (AuthenticatedPrincipal) servletRequest.getAttribute(
                AuthenticatedPrincipal.class.getName()
        );

        if (principal == null) { return next.handle(request); }

        ServerRequest modifiedRequest = ServerRequest.from(request)
                .header(HttpHeaders.USER_ID, principal.userId().toString())
                .header(HttpHeaders.USER_EMAIL, principal.email())
                .build();

        return next.handle(modifiedRequest);
    }
}
