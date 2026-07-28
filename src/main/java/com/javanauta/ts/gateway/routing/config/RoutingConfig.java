package com.javanauta.ts.gateway.routing.config;

import com.javanauta.ts.gateway.routing.filter.AuthenticatedPrincipalHeaderFilter;
import com.javanauta.ts.gateway.properties.GatewayServiceProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.web.servlet.function.RequestPredicates.POST;
import static org.springframework.web.servlet.function.RequestPredicates.path;

@Configuration
@RequiredArgsConstructor
public class RoutingConfig {
    private final GatewayServiceProperties properties;
    private final AuthenticatedPrincipalHeaderFilter headerFilter;

    @Bean
    RouterFunction<ServerResponse> gatewayRoutes() {
        return GatewayRouterFunctions.route("gateway")

                // Public endpoints

                .route(POST("/api/v1/users"), HandlerFunctions.http())
                .before(uri(properties.userServiceUri()))

                .route(path("/api/v1/ceps/**"), HandlerFunctions.http())
                .before(uri(properties.userServiceUri()))

                .route(path("/api/v1/auth/**"), HandlerFunctions.http())
                .before(uri(properties.userServiceUri()))

                // Protected User endpoints

                .route(path("/api/v1/users/**"), HandlerFunctions.http())
                .before(uri(properties.userServiceUri()))
                .filter(headerFilter)

                // Protected Task endpoints

                .route(path("/api/v1/tasks/**"), HandlerFunctions.http())
                .before(uri(properties.taskServiceUri()))
                .filter(headerFilter)

                .build();
    }
}