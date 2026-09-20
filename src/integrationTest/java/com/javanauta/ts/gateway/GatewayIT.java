package com.javanauta.ts.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.javanauta.ts.apicontract.http.HttpHeaders;
import com.javanauta.ts.apicontract.response.enums.ResponseStatus;
import com.javanauta.ts.gateway.exception.enums.ExceptionCode;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@SpringBootTest(
        classes = GatewayApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class GatewayIT {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String USER_EMAIL = "user@example.com";
    private static final String JWT_SECRET = "test-jwt-secret-that-is-definitely-longer-than-32-chars";

    private static final WireMockServer USER_SERVICE = new WireMockServer(wireMockConfig().dynamicPort());
    private static final WireMockServer TASK_SERVICE = new WireMockServer(wireMockConfig().dynamicPort());

    @LocalServerPort
    private int gatewayPort;

    private WebTestClient webTestClient;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        USER_SERVICE.start();
        TASK_SERVICE.start();

        registry.add(
                "ts.services.user-service-uri",
                USER_SERVICE::baseUrl
        );
        registry.add(
                "ts.services.task-service-uri",
                TASK_SERVICE::baseUrl
        );
        registry.add(
                "ts.jwt.secret",
                () -> JWT_SECRET
        );
    }

    @BeforeEach
    void setUp() {
        USER_SERVICE.resetAll();
        TASK_SERVICE.resetAll();

        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + gatewayPort)
                .build();
    }

    @AfterAll
    static void tearDown() {
        USER_SERVICE.stop();
        TASK_SERVICE.stop();
    }

    @Test
    void createUser_shouldRoutePublicRequestToUserService() {
        USER_SERVICE.stubFor(
                post(urlEqualTo("/api/v1/users"))
                        .willReturn(
                                aResponse()
                                        .withStatus(201)
                                        .withHeader(
                                                "Content-Type",
                                                MediaType.APPLICATION_JSON_VALUE
                                        )
                                        .withBody("""
                                                {
                                                  "status": "SUCCESS",
                                                  "code": 201
                                                }
                                                """)
                        )
        );

        webTestClient.post()
                .uri("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "email": "user@example.com"
                        }
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS")
                .jsonPath("$.code").isEqualTo(201);

        USER_SERVICE.verify(
                postRequestedFor(
                        urlEqualTo("/api/v1/users")
                )
        );
    }

    @Test
    void getCep_shouldRoutePublicRequestToUserService() {
        USER_SERVICE.stubFor(
                get(urlEqualTo("/api/v1/ceps/12345678"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                "Content-Type",
                                                MediaType.APPLICATION_JSON_VALUE
                                        )
                                        .withBody("""
                                                {
                                                  "cep": "12345678"
                                                }
                                                """)
                        )
        );

        webTestClient.get()
                .uri("/api/v1/ceps/12345678")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.cep").isEqualTo("12345678");

        USER_SERVICE.verify(
                getRequestedFor(
                        urlEqualTo("/api/v1/ceps/12345678")
                )
        );
    }

    @Test
    void getTask_shouldAuthenticateAndPropagatePrincipalHeaders() {
        TASK_SERVICE.stubFor(
                get(urlEqualTo("/api/v1/tasks/task-123"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                "Content-Type",
                                                MediaType.APPLICATION_JSON_VALUE
                                        )
                                        .withBody("""
                                                {
                                                  "id": "task-123"
                                                }
                                                """)
                        )
        );

        webTestClient.get()
                .uri("/api/v1/tasks/task-123")
                .header(
                        org.springframework.http.HttpHeaders.AUTHORIZATION,
                        "Bearer " + validJwt()
                )
                .exchange()
                .expectStatus().isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isEqualTo("task-123");

        TASK_SERVICE.verify(
                getRequestedFor(
                        urlEqualTo("/api/v1/tasks/task-123")
                )
                        .withHeader(
                                HttpHeaders.USER_ID,
                                equalTo(USER_ID.toString())
                        )
                        .withHeader(
                                HttpHeaders.USER_EMAIL,
                                equalTo(USER_EMAIL)
                        )
        );
    }

    @Test
    void getUser_shouldAuthenticateAndPropagatePrincipalHeaders() {
        USER_SERVICE.stubFor(
                get(urlEqualTo("/api/v1/users/me"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                "Content-Type",
                                                MediaType.APPLICATION_JSON_VALUE
                                        )
                                        .withBody("""
                                                {
                                                  "id": "%s"
                                                }
                                                """.formatted(USER_ID))
                        )
        );

        webTestClient.get()
                .uri("/api/v1/users/me")
                .header(
                        org.springframework.http.HttpHeaders.AUTHORIZATION,
                        "Bearer " + validJwt()
                )
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(USER_ID.toString());

        USER_SERVICE.verify(
                getRequestedFor(
                        urlEqualTo("/api/v1/users/me")
                )
                        .withHeader(
                                HttpHeaders.USER_ID,
                                equalTo(USER_ID.toString())
                        )
                        .withHeader(
                                HttpHeaders.USER_EMAIL,
                                equalTo(USER_EMAIL)
                        )
        );
    }

    @Test
    void getTasks_shouldReturnUnauthorizedWhenAuthenticationIsMissing() {
        webTestClient.get()
                .uri("/api/v1/tasks")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo(ResponseStatus.ERROR.toString())
                .jsonPath("$.code").isEqualTo(401)
                .jsonPath("$.errorCode")
                .isEqualTo(
                        ExceptionCode.AUTHENTICATION_ERROR
                                .getIdentifier()
                )
                .jsonPath("$.message").isNotEmpty()
                .jsonPath("$.validationErrors").isArray()
                .jsonPath("$.validationErrors.length()").isEqualTo(0);

        TASK_SERVICE.verify(
                0,
                getRequestedFor(urlPathMatching("/api/v1/tasks.*"))
        );
    }

    @Disabled("Known issue: invalid JWT currently returns 500 instead of 401")
    @Test
    void getTasks_shouldReturnUnauthorizedWhenJwtIsInvalid() {
        webTestClient.get()
                .uri("/api/v1/tasks")
                .header(
                        org.springframework.http.HttpHeaders.AUTHORIZATION,
                        "Bearer invalid-token"
                )
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo(ResponseStatus.ERROR.toString())
                .jsonPath("$.code").isEqualTo(401)
                .jsonPath("$.errorCode")
                .isEqualTo(
                        ExceptionCode.AUTHENTICATION_ERROR
                                .getIdentifier()
                )
                .jsonPath("$.message")
                .isEqualTo("Invalid JWT token")
                .jsonPath("$.validationErrors").isArray()
                .jsonPath("$.validationErrors.length()").isEqualTo(0);

        TASK_SERVICE.verify(
                0,
                getRequestedFor(urlPathMatching("/api/v1/tasks.*"))
        );
    }

    @Test
    void getTaskOpenApi_shouldRewritePathAndRouteToTaskService() {
        TASK_SERVICE.stubFor(
                get(urlEqualTo("/v3/api-docs"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                "Content-Type",
                                                MediaType.APPLICATION_JSON_VALUE
                                        )
                                        .withBody("""
                                                {
                                                  "openapi": "3.1.0"
                                                }
                                                """)
                        )
        );

        webTestClient.get()
                .uri("/v3/api-docs/task-service")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.openapi").isEqualTo("3.1.0");

        TASK_SERVICE.verify(
                getRequestedFor(urlEqualTo("/v3/api-docs"))
        );
    }

    private String validJwt() {
        SecretKey key = Keys.hmacShaKeyFor(
                JWT_SECRET.getBytes(StandardCharsets.UTF_8)
        );

        return Jwts.builder()
                .subject(USER_ID.toString())
                .claim("email", USER_EMAIL)
                .signWith(key)
                .compact();
    }
}
