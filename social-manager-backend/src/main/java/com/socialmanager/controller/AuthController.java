package com.socialmanager.controller;

import com.socialmanager.dto.LoginRequest;
import com.socialmanager.dto.LoginResponse;
import com.socialmanager.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtUtil jwtUtil;
    private final String expectedUsername;
    private final String expectedPassword;

    public AuthController(JwtUtil jwtUtil,
                          @Value("${app.auth.username}") String expectedUsername,
                          @Value("${app.auth.password}") String expectedPassword) {
        this.jwtUtil = jwtUtil;
        this.expectedUsername = expectedUsername;
        this.expectedPassword = expectedPassword;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        if (!expectedUsername.equals(request.getUsername()) || !expectedPassword.equals(request.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        String token = jwtUtil.generateToken(request.getUsername());
        return ResponseEntity.ok(new LoginResponse(token, request.getUsername()));
    }
}