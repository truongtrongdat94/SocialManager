package com.socialmanager.security;

import com.socialmanager.model.User;
import com.socialmanager.service.AuthService;
import com.socialmanager.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();
        String googleId = oidcUser.getSubject();

        User user = authService.processOAuthUser(email, name, googleId);
        
        // Generate both access token and refresh token
        String accessToken = jwtUtil.generateToken(email);
        String refreshToken = jwtUtil.generateRefreshToken(email);
        
        log.info("🔑 Generated tokens for user: {}", email);
        log.info("   Access Token (first 20 chars): {}", accessToken.substring(0, Math.min(20, accessToken.length())));
        log.info("   Refresh Token (first 20 chars): {}", refreshToken.substring(0, Math.min(20, refreshToken.length())));
        log.info("   Tokens are same: {}", accessToken.equals(refreshToken));
        
        // Save refresh token to database
        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiresAt(
            LocalDateTime.now().plusSeconds(jwtUtil.getRefreshExpirationMs() / 1000)
        );
        authService.saveUser(user);
        
        // Set refresh token as httpOnly cookie
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false); // Set true in production with HTTPS
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge((int) (jwtUtil.getRefreshExpirationMs() / 1000)); // 7 days
        refreshTokenCookie.setAttribute("SameSite", "Lax");
        response.addCookie(refreshTokenCookie);
        
        // Redirect to frontend with ONLY access token
        String redirectUrl = UriComponentsBuilder
            .fromUriString("http://localhost:3000/auth/callback")
            .queryParam("accessToken", accessToken)
            .build()
            .toUriString();
            
        response.sendRedirect(redirectUrl);
    }
}
