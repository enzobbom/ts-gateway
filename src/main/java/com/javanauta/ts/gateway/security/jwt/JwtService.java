package com.javanauta.ts.gateway.security.jwt;

import com.javanauta.ts.gateway.properties.JwtProperties;
import com.javanauta.ts.gateway.security.principal.AuthenticatedPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class JwtService {
    private final SecretKey secretKey;

    public JwtService(JwtProperties properties) {
        this.secretKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public AuthenticatedPrincipal authenticate(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new AuthenticatedPrincipal(
                UUID.fromString(claims.getSubject()),
                claims.get("email", String.class)
        );
    }
}