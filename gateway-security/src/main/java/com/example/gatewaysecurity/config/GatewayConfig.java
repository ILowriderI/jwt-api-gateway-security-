package com.example.gatewaysecurity.config;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor

public class GatewayConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("public_route", r -> r
                        .path("/public/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("http://localhost:8081"))
                .route("secure_route", r -> r
                        .path("/secure/**")
                        .filters(f -> f.stripPrefix(1).filter(jwtAuthFilter.apply(new JwtAuthFilter.Config())))
                        .uri("http://localhost:8082"))
                .build();
    }
}
