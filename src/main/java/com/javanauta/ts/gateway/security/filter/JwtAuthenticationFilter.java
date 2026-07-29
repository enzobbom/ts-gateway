package com.javanauta.ts.gateway.security.filter;

import com.javanauta.ts.gateway.security.jwt.JwtService;
import com.javanauta.ts.gateway.security.principal.AuthenticatedPrincipal;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();

        if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")) {
            return true;
        }

        if (HttpMethod.POST.matches(method)) {
            return path.equals("/api/v1/auth/login") || path.equals("/api/v1/users");
        }

        if (HttpMethod.GET.matches(method)) {
            return path.startsWith("/api/v1/ceps/");
        }

        return path.equals("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization == null) {
            throw new BadCredentialsException("Missing Authorization header");
        }

        if (!authorization.startsWith("Bearer ")) {
            throw new BadCredentialsException("Invalid Authorization header");
        }

        try {
            String token = authorization.substring(7);

            AuthenticatedPrincipal principal = jwtService.authenticate(token);
            request.setAttribute(
                    AuthenticatedPrincipal.class.getName(),
                    principal
            );

            filterChain.doFilter(request, response);

        } catch (JwtException | IllegalArgumentException ex) {
            throw new BadCredentialsException("Invalid JWT token", ex);
        }
    }
}