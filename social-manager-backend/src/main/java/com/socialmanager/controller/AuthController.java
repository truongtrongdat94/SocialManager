package com.socialmanager.controller;

import com.socialmanager.dto.ApiResponse;
import com.socialmanager.dto.AuthResponse;
import com.socialmanager.dto.LoginRequest;
import com.socialmanager.dto.RegisterRequest;
import com.socialmanager.dto.UserDto;
import com.socialmanager.service.AuthService;
import com.socialmanager.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDto>> register(@Valid @RequestBody RegisterRequest request) {
        UserDto userDto = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(userDto));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.login(request);
        
        // Set refresh token as httpOnly cookie
        Cookie refreshTokenCookie = new Cookie("refreshToken", authResponse.refreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false); // Set true in production with HTTPS
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge((int) (jwtUtil.getRefreshExpirationMs() / 1000));
        refreshTokenCookie.setAttribute("SameSite", "Lax");
        response.addCookie(refreshTokenCookie);
        
        // Return only access token in response body
        AuthResponse responseBody = new AuthResponse(authResponse.accessToken(), null);
        return ResponseEntity.ok(ApiResponse.ok(responseBody));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // Get refresh token from cookie
        String refreshToken = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }
        
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Refresh token not found"));
        }
        
        AuthResponse authResponse = authService.refreshAccessToken(refreshToken);
        
        // Set new refresh token as httpOnly cookie
        Cookie refreshTokenCookie = new Cookie("refreshToken", authResponse.refreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge((int) (jwtUtil.getRefreshExpirationMs() / 1000));
        refreshTokenCookie.setAttribute("SameSite", "Lax");
        response.addCookie(refreshTokenCookie);
        
        // Return only access token in response body
        AuthResponse responseBody = new AuthResponse(authResponse.accessToken(), null);
        return ResponseEntity.ok(ApiResponse.ok(responseBody));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> me(Authentication authentication) {
        UserDto userDto = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(userDto));
    }
}
