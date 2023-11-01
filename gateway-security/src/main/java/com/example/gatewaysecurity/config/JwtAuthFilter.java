package com.example.gatewaysecurity.config;

import com.example.gatewaysecurity.repository.TokenRepository;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor

public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    private final TokenRepository tokenRepository;

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                var isTokenValid = tokenRepository.findByToken(jwt)
                        .map(token -> !token.getExpired() && !token.getRevoked())
                        .orElse(false);
                if (isTokenValid) {
                    return chain.filter(exchange);
                }
            }


            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        };
    }


    public static class Config {

    }


}
