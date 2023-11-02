package com.example.gatewaysecurity.controller;

import com.example.gatewaysecurity.service.AuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RefreshTokenRouter {

    @Bean
    public RouterFunction<ServerResponse> refreshTokenRoute(AuthService service) {
        return route()
                .POST("/auth/refresh-token", service::refreshToken)
                .build();
    }
}
