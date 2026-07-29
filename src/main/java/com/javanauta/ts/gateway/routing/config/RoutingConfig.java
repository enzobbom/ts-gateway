package com.javanauta.ts.gateway.routing.config;

import com.javanauta.ts.gateway.properties.GatewayServiceProperties;
import com.javanauta.ts.gateway.routing.filter.AuthenticatedPrincipalHeaderFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.rewritePath;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.web.servlet.function.RequestPredicates.*;

@Configuration
@RequiredArgsConstructor
public class RoutingConfig {
    private final GatewayServiceProperties properties;
    private final AuthenticatedPrincipalHeaderFilter headerFilter;

    @Bean
    RouterFunction<ServerResponse> publicRoutes() {
        return GatewayRouterFunctions.route("public")
                .route(POST("/api/v1/users"), HandlerFunctions.http())
                .before(uri(properties.userServiceUri()))

                .route(path("/api/v1/ceps/**"), HandlerFunctions.http())
                .before(uri(properties.userServiceUri()))

                .route(path("/api/v1/auth/**"), HandlerFunctions.http())
                .before(uri(properties.userServiceUri()))

                .build();
    }

    @Bean
    RouterFunction<ServerResponse> openApiUserRoute() {
        return GatewayRouterFunctions.route("openapi-user")
                .route(GET("/v3/api-docs/user-service"), HandlerFunctions.http())
                .before(uri(properties.userServiceUri()))
                .before(rewritePath("/v3/api-docs/user-service", "/v3/api-docs"))

                .build();
    }

    @Bean
    RouterFunction<ServerResponse> openApiTaskRoute() {
        return GatewayRouterFunctions.route("openapi-task")
                .route(GET("/v3/api-docs/task-service"), HandlerFunctions.http())
                .before(uri(properties.taskServiceUri()))
                .before(rewritePath("/v3/api-docs/task-service", "/v3/api-docs"))

                .build();
    }

    @Bean
    RouterFunction<ServerResponse> userRoutes() {
        return GatewayRouterFunctions.route("user")
                .route(path("/api/v1/users/**"), HandlerFunctions.http())
                .before(uri(properties.userServiceUri()))
                .filter(headerFilter)

                .build();
    }

    @Bean
    RouterFunction<ServerResponse> taskRoutes() {
        return GatewayRouterFunctions.route("task")
                .route(path("/api/v1/tasks/**"), HandlerFunctions.http())
                .before(uri(properties.taskServiceUri()))
                .filter(headerFilter)

                .build();
    }
}
