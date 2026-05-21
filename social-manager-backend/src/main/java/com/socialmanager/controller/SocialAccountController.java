package com.socialmanager.controller;

import com.socialmanager.client.TikTokClient;
import com.socialmanager.dto.ApiResponse;
import com.socialmanager.dto.request.FacebookPublishRequest;
import com.socialmanager.dto.SocialAccountDto;
import com.socialmanager.exception.CsrfSecurityException;
import com.socialmanager.exception.OAuthCallbackException;
import com.socialmanager.model.Platform;
import com.socialmanager.service.SocialAccountService;
import com.socialmanager.util.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/social-accounts")
@RequiredArgsConstructor
public class SocialAccountController {
    @Value("${FRONTEND_URL:http://localhost:3000}")
    private String frontendUrl;

    private final JwtUtil jwtUtil;
    private final SocialAccountService socialAccountService;

    @GetMapping("/connect/{platform}")
    public ResponseEntity<ApiResponse<String>> getConnectUrl(@PathVariable Platform platform, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthenticated"));
        }
        try {
            String username = authentication.getName();
            String url = socialAccountService.generateAuthUrl(platform, username);
            return ResponseEntity.ok(ApiResponse.ok(url));
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "Failed to generate connect URL";
            return ResponseEntity.badRequest().body(ApiResponse.error(msg));
        }
    }

    @GetMapping("/callback/facebook")
    public void handleFacebookCallback(
        @RequestParam(name = "code", required = false) String code,
        @RequestParam(name = "error", required = false) String error,
        @RequestParam(name = "state", required = false) String state,
        HttpServletResponse response
    ) throws IOException {
        if (error != null) {
            response.sendRedirect(frontendUrl + "/dashboard/accounts?status=error&reason=" + java.net.URLEncoder.encode(error, java.nio.charset.StandardCharsets.UTF_8));
            return;
        }

        try {
            String username = jwtUtil.getUsernameFromToken(state);
            socialAccountService.connectFacebookAccount(code, username);
            response.sendRedirect(frontendUrl + "/dashboard/accounts?status=success");
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "unknown";
            response.sendRedirect(frontendUrl + "/dashboard/accounts?status=error&reason=" + java.net.URLEncoder.encode(msg, java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    @GetMapping("/callback/instagram")
    public void handleInstagramCallback(
        @RequestParam(name = "code", required = false) String code,
        @RequestParam(name = "error", required = false) String error,
        @RequestParam(name = "state", required = false) String state,
        HttpServletResponse response
    ) throws IOException {
        if (error != null) {
            response.sendRedirect(frontendUrl + "/dashboard/accounts?status=error&reason=" + java.net.URLEncoder.encode(error, java.nio.charset.StandardCharsets.UTF_8));
            return;
        }

        try {
            String username = jwtUtil.getUsernameFromToken(state);
            socialAccountService.connectInstagramAccount(code, username);
            response.sendRedirect(frontendUrl + "/dashboard/accounts?status=success");
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "unknown";
            response.sendRedirect(frontendUrl + "/dashboard/accounts?status=error&reason=" + java.net.URLEncoder.encode(msg, java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    @GetMapping("/callback/threads")
    public void handleThreadsCallback(
        @RequestParam(name = "code", required = false) String code,
        @RequestParam(name = "error", required = false) String error,
        @RequestParam(name = "state", required = false) String state,
        HttpServletResponse response
    ) throws IOException {
        if (error != null) {
            response.sendRedirect(frontendUrl + "/dashboard/accounts?status=error&reason=" + java.net.URLEncoder.encode(error, java.nio.charset.StandardCharsets.UTF_8));
            return;
        }

        try {
            String username = jwtUtil.getUsernameFromToken(state);
            socialAccountService.connectThreadsAccount(code, username);
            response.sendRedirect(frontendUrl + "/dashboard/accounts?status=success");
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "unknown";
            response.sendRedirect(frontendUrl + "/dashboard/accounts?status=error&reason=" + java.net.URLEncoder.encode(msg, java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    @GetMapping("/callback/tiktok")
    public void handleTikTokCallback(
        @RequestParam(name = "code", required = false) String code,
        @RequestParam(name = "error", required = false) String error,
        @RequestParam(name = "state", required = false) String state,
        HttpServletResponse response
    ) throws IOException {
        if (error != null) {
            response.sendRedirect(frontendUrl + "/dashboard/accounts?status=error&reason=" + java.net.URLEncoder.encode(error, java.nio.charset.StandardCharsets.UTF_8));
            return;
        }

        try {
            if (state == null || !TikTokClient.pkceStorage.containsKey(state)) {
                throw new CsrfSecurityException("Lỗi: State không khớp hoặc đã hết hạn!");
            }

            String codeVerifier = TikTokClient.pkceStorage.remove(state);
            String username = jwtUtil.getUsernameFromToken(state);

            socialAccountService.connectTikTokAccount(code, codeVerifier, username);
            response.sendRedirect(frontendUrl + "/dashboard/accounts?status=success");
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "unknown";
            response.sendRedirect(frontendUrl + "/dashboard/accounts?status=error&reason=" + java.net.URLEncoder.encode(msg, java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SocialAccountDto>>> getMySocialAccounts(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthenticated"));
        }
        String username = authentication.getName();
        return ResponseEntity.ok(
            ApiResponse.ok(
                socialAccountService.getSocialAccountsByUsername(username)
            )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SocialAccountDto>> getMySocialAccountById(
        @PathVariable UUID id,
        Authentication authentication
    ) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthenticated"));
        }
        String username = authentication.getName();
        return ResponseEntity.ok(
            ApiResponse.ok(
                socialAccountService.getSocialAccountByIdAndUsername(id, username)
            )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteSocialAccount(
        @PathVariable UUID id,
        Authentication authentication
    ) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthenticated"));
        }
        String username = authentication.getName();
        socialAccountService.deleteSocialAccountById(id, username);
        return ResponseEntity.ok(
            ApiResponse.ok("Social account deleted successfully")
        );
    }

    @PostMapping("/{id}/facebook/publish")
    public ResponseEntity<ApiResponse<String>> publishFacebookPost(
        @PathVariable UUID id,
        @RequestBody FacebookPublishRequest request,
        Authentication authentication
    ) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthenticated"));
        }
        String username = authentication.getName();
        String publishedId = socialAccountService.publishFacebookPost(id, username, request.getCaption(), request.getMediaUrls());

        return ResponseEntity.ok(
            ApiResponse.ok(publishedId)
        );
    }
}