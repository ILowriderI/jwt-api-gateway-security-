package com.example.gatewaysecurity.router;

import com.example.gatewaysecurity.service.AuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class AuthRouter {


    @Bean
    public RouterFunction<ServerResponse> refreshTokenRoute(AuthService service) {
        return route()
                .POST("/auth/refresh-token", service::refreshToken)
                .POST("/auth/logout", service::logout)
                .POST("/auth/register", service::register)
                .POST("/auth/authenticate" , service::authenticate)

                .build();
    }
}
