package com.example.gatewaysecurity.service;

import com.example.gatewaysecurity.enums.Role;
import com.example.gatewaysecurity.enums.TokenType;
import com.example.gatewaysecurity.model.*;
import com.example.gatewaysecurity.repository.RefreshTokenRepository;
import com.example.gatewaysecurity.repository.TokenRepository;
import com.example.gatewaysecurity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtService jwtService;


//    public AuthResponse register(RegisterRequest request) {
//
//        var user = User.builder()
//                .firstname(request.getFirstname())
//                .lastname(request.getLastname())
//                .email(request.getEmail())
//                .password(passwordEncoder.encode(request.getPassword()))
//                .role(Role.USER)
//                .build();
//        userRepository.save(user);
//        var jwtToken = jwtService.generateToken(user);
//        var refreshToken = jwtService.generateRefreshToken(user);
//        saveUserToken(user, jwtToken);
//        saveRefreshUserToken(user,refreshToken);
//
//        return AuthResponse.builder()
//                .accessToken(jwtToken)
//                .refreshToken(refreshToken)
//                .build();
//    }

    public Mono<ServerResponse> register(ServerRequest request) {

        Mono<RegisterRequest> registerRequestMono = request.bodyToMono(RegisterRequest.class);

        return registerRequestMono.flatMap(registerRequest -> {
            var user = User.builder()
                    .firstname(registerRequest.getFirstname())
                    .lastname(registerRequest.getLastname())
                    .email(registerRequest.getEmail())
                    .password(passwordEncoder.encode(registerRequest.getPassword()))
                    .role(Role.USER)
                    .build();
            userRepository.save(user);
            var jwtToken = jwtService.generateToken(user);
            var refreshToken = jwtService.generateRefreshToken(user);
            saveUserToken(user, jwtToken);
            saveRefreshUserToken(user, refreshToken);

            // Возвращение ответа
            return ServerResponse.ok().bodyValue(AuthResponse.builder()
                    .accessToken(jwtToken)
                    .refreshToken(refreshToken)
                    .build());
        });
    }


    private void saveUserToken(User user, String jwtToken) {
        var token = Token.builder()
                .user(user)
                .token(jwtToken)
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }

    private void saveRefreshUserToken(User user, String jwtToken) {
        var token = RefreshToken.builder()
                .user(user)
                .token(jwtToken)
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .build();
        refreshTokenRepository.save(token);
    }

    private void revokeAllUserTokens(User user) {
        var validTokens = tokenRepository.findAllTokensByUser(user.getId());
        if (validTokens.isEmpty()) return;
        validTokens.forEach(token -> {
            token.setRevoked(true);
            token.setExpired(true);
        });
        tokenRepository.saveAll(validTokens);
    }

    private void revokeAllRefreshUserTokens(User user) {
        var validTokens = refreshTokenRepository.findAllTokensByUser(user.getId());
        if (validTokens.isEmpty()) return;
        validTokens.forEach(token -> {
            token.setRevoked(true);
            token.setExpired(true);
        });
        refreshTokenRepository.saveAll(validTokens);
    }


//    public AuthResponse authenticate(AuthRequest request) {
//        var user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new NoSuchElementException("User not found"));
//
//
//        boolean isPasswordValid = passwordEncoder.matches(request.getPassword(), user.getPassword());
//        if (isPasswordValid) {
//            revokeAllUserTokens(user);
//            revokeAllRefreshUserTokens(user);
//            var jwtToken = jwtService.generateToken(user);
//            var refreshToken = jwtService.generateRefreshToken(user);
//            saveUserToken(user, jwtToken);
//            saveRefreshUserToken(user,refreshToken);
//
//            return AuthResponse.builder()
//                    .accessToken(jwtToken)
//                    .refreshToken(refreshToken)
//                    .build();
//        } else {
//            throw new IllegalArgumentException("Invalid password");
//        }
//    }

    public Mono<ServerResponse> authenticate(ServerRequest request) {
        Mono<AuthRequest> authRequestMono = request.bodyToMono(AuthRequest.class);

        return authRequestMono.flatMap(authRequest -> {
            var user = userRepository.findByEmail(authRequest.getEmail())
                    .orElseThrow(() -> new NoSuchElementException("User not found"));

            boolean isPasswordValid = passwordEncoder.matches(authRequest.getPassword(), user.getPassword());
            if (isPasswordValid) {
                revokeAllUserTokens(user);
                revokeAllRefreshUserTokens(user);
                var jwtToken = jwtService.generateToken(user);
                var refreshToken = jwtService.generateRefreshToken(user);
                saveUserToken(user, jwtToken);
                saveRefreshUserToken(user, refreshToken);

                return ServerResponse.ok().bodyValue(AuthResponse.builder()
                        .accessToken(jwtToken)
                        .refreshToken(refreshToken)
                        .build());
            } else {
                return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
            }
        });
    }




    public Mono<ServerResponse> refreshToken(ServerRequest request) {
        return request.headers().header(HttpHeaders.AUTHORIZATION)
                .stream()
                .findFirst()
                .map(authHeader -> {
                    if (!authHeader.startsWith("Bearer ")) {
                        return ServerResponse.badRequest().build();
                    }
                    String refreshToken = authHeader.substring(7);
                    String userEmail = jwtService.extractUserName(refreshToken);

                    if (userEmail != null) {
                        User user = userRepository.findByEmail(userEmail).orElseThrow();

                        boolean isTokenValid = refreshTokenRepository.findByToken(refreshToken)
                                .map(token -> !token.getExpired() && !token.getRevoked())
                                .orElse(false);

                        if (jwtService.isTokenValid(refreshToken, user) && isTokenValid) {
                            String accessToken = jwtService.generateToken(user);
                            revokeAllUserTokens(user);
                            saveUserToken(user, accessToken);

                            AuthResponse authResponse = AuthResponse.builder()
                                    .accessToken(accessToken)
                                    .refreshToken(refreshToken)
                                    .build();

                            return ServerResponse.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .bodyValue(authResponse);
                        }
                    }

                    return ServerResponse.badRequest().build();
                })
                .orElse(ServerResponse.badRequest().build());
    }



    public Mono<ServerResponse> logout(ServerRequest request) {
        return request.headers().header(HttpHeaders.AUTHORIZATION)
                .stream()
                .findFirst()
                .map(authHeader -> {
                    if (!authHeader.startsWith("Bearer ")) {
                        return ServerResponse.badRequest().build();
                    }
                    String jwt = authHeader.substring(7);
                    boolean isTokenValid = tokenRepository.findByToken(jwt)
                            .map(token -> !token.getExpired() && !token.getRevoked())
                            .orElse(false);

                    String userEmail = jwtService.extractUserName(jwt);
                    User user = userRepository.findByEmail(userEmail).orElse(null);

                    if (user != null && isTokenValid && jwtService.isTokenValid(jwt, user)) {
                        revokeAllRefreshUserTokens(user);
                        revokeAllUserTokens(user);
                        return ServerResponse.ok().build();
                    } else {
                        return ServerResponse.badRequest().build();
                    }
                })
                .orElse(ServerResponse.badRequest().build());
    }



}
