package com.javanauta.ts.gateway.security.filter;

import com.javanauta.ts.gateway.security.jwt.JwtService;
import com.javanauta.ts.gateway.security.principal.AuthenticatedPrincipal;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String USER_EMAIL = "user@example.com";
    private static final String TOKEN = "test-token";

    @Mock
    private JwtService jwtService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter underTest;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @CsvSource({
            "GET, /v3/api-docs/user-service",
            "GET, /swagger-ui/index.html",
            "GET, /actuator/health",
            "GET, /error",
            "POST, /api/v1/auth/login",
            "POST, /api/v1/users",
            "GET, /api/v1/ceps/12345678"
    })
    void doFilter_shouldSkipAuthenticationForPublicRequest(String method, String path) throws Exception {
        MockHttpServletRequest request = request(method, path);
        MockHttpServletResponse response = new MockHttpServletResponse();

        underTest.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_shouldContinueWithoutAuthenticationWhenAuthorizationHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = request("GET", "/api/v1/tasks");
        MockHttpServletResponse response = new MockHttpServletResponse();

        underTest.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);

        assertThat(request.getAttribute(
                AuthenticatedPrincipal.class.getName()
        )).isNull();

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_shouldContinueWithoutAuthenticationWhenAuthorizationHeaderIsNotBearer() throws Exception {
        MockHttpServletRequest request = request("GET", "/api/v1/tasks");
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic credentials");

        underTest.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);

        assertThat(request.getAttribute(
                AuthenticatedPrincipal.class.getName()
        )).isNull();

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_shouldAuthenticateAndSetPrincipalForValidBearerToken() throws Exception {
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(USER_ID, USER_EMAIL);

        MockHttpServletRequest request = request("GET", "/api/v1/tasks");
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + TOKEN
        );

        when(jwtService.authenticate(TOKEN)).thenReturn(principal);

        underTest.doFilter(request, response, filterChain);

        assertThat(request.getAttribute(
                AuthenticatedPrincipal.class.getName()
        )).isSameAs(principal);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isSameAs(principal);
        assertThat(authentication.getCredentials()).isNull();
        assertThat(authentication.getAuthorities()).isEmpty();
        assertThat(authentication.isAuthenticated()).isTrue();

        verify(jwtService).authenticate(TOKEN);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_shouldThrowBadCredentialsExceptionForInvalidBearerToken() {
        MockHttpServletRequest request = request("GET", "/api/v1/tasks");
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + TOKEN
        );

        MalformedJwtException jwtException = new MalformedJwtException("Malformed JWT");

        when(jwtService.authenticate(TOKEN)).thenThrow(jwtException);

        assertThatThrownBy(
                () -> underTest.doFilter(request, response, filterChain)
        )
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid JWT token")
                .hasCause(jwtException);

        verify(jwtService).authenticate(TOKEN);
        verifyNoInteractions(filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }
}
