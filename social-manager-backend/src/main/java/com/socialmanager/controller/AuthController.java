package com.socialmanager.controller;

import com.socialmanager.dto.ApiResponse;
import com.socialmanager.dto.AuthResponse;
import com.socialmanager.dto.LoginRequest;
import com.socialmanager.dto.RegisterRequest;
import com.socialmanager.dto.UserDto;
import com.socialmanager.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDto>> register(@Valid @RequestBody RegisterRequest request) {
        UserDto userDto = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(userDto));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(new AuthResponse(token)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> me(Authentication authentication) {
        UserDto userDto = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(userDto));
    }
}
