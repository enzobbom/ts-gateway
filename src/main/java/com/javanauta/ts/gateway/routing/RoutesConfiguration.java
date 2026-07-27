package com.javanauta.ts.gateway.routing;

import com.javanauta.ts.gateway.properties.GatewayServiceProperties;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.web.servlet.function.RequestPredicates.path;

@Configuration
public class RoutesConfiguration {

    @Bean
    RouterFunction<ServerResponse> gatewayRoutes(GatewayServiceProperties properties) {
        return GatewayRouterFunctions.route("gateway")
                .route(path("/api/v1/users/**"), HandlerFunctions.http())
                .before(uri(properties.userServiceUri()))
                .route(path("/api/v1/ceps/**"), HandlerFunctions.http())
                .before(uri(properties.userServiceUri()))
                .route(path("/api/v1/auth/**"), HandlerFunctions.http())
                .before(uri(properties.userServiceUri()))
                .route(path("/api/v1/tasks/**"), HandlerFunctions.http())
                .before(uri(properties.taskServiceUri()))
                .build();
    }
}