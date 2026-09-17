package com.booksquest.api.auth.service;

import com.booksquest.api.auth.dto.AuthResponse;
import com.booksquest.api.auth.dto.LoginRequest;
import com.booksquest.api.auth.dto.RegisterRequest;
import com.booksquest.api.auth.entity.User;
import com.booksquest.api.auth.exception.AuthException;
import com.booksquest.shared.util.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    @Autowired
    private UserService userService;

    @Autowired
    private JwtProvider jwtProvider;

    public AuthResponse register(RegisterRequest request) {
        User user = userService.createUser(request.getEmail(), request.getUsername(), request.getPassword());
        String token = jwtProvider.generateToken(user.getId(), user.getEmail());
        
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .expiresIn(jwtProvider.getExpirationTimeInMillis())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userService.findByEmail(request.getEmail());
        
        if (!userService.verifyPassword(request.getPassword(), user.getPasswordHash())) {
            throw new AuthException("Invalid email or password");
        }

        String token = jwtProvider.generateToken(user.getId(), user.getEmail());
        
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .expiresIn(jwtProvider.getExpirationTimeInMillis())
                .build();
    }
}
