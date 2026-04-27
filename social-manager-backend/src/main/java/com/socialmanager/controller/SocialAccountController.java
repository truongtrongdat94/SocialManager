package com.socialmanager.controller;

import com.socialmanager.client.TikTokClient;
import com.socialmanager.dto.ApiResponse;
import com.socialmanager.dto.SocialAccountDto;
import com.socialmanager.model.Platform;
import com.socialmanager.service.account.SocialAccountService;
import com.socialmanager.util.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/social-accounts")
@RequiredArgsConstructor
public class SocialAccountController {
    private final JwtUtil jwtUtil;
    private final SocialAccountService socialAccountService;

    @GetMapping("/connect/{platform}")
    public ResponseEntity<ApiResponse<String>> getConnectUrl(@PathVariable Platform platform, Authentication authentication) {
        String username = authentication.getName();
        String url = socialAccountService.generateAuthUrl(platform, username);
        return ResponseEntity.ok(ApiResponse.ok(url));
    }

    @GetMapping("/callback/facebook")
    public void handleFacebookCallback(
        @RequestParam(name = "code", required = false) String code,
        @RequestParam(name = "error", required = false) String error,
        @RequestParam(name = "state", required = false) String state,
        HttpServletResponse response
    ) throws Exception {
        if (error != null) {
            response.sendRedirect("http://localhost:3000/failed");
            return;
        }

        String username = jwtUtil.getUsernameFromToken(state);
        socialAccountService.connectFacebookAccount(code, username);
        response.sendRedirect("http://localhost:3000/success");
    }

    @GetMapping("/callback/instagram")
    public void handleInstagramCallback(
        @RequestParam(name = "code", required = false) String code,
        @RequestParam(name = "error", required = false) String error,
        @RequestParam(name = "state", required = false) String state,
        HttpServletResponse response
    ) throws Exception {
        if (error != null) {
            response.sendRedirect("http://localhost:3000/failed");
            return;
        }

        String username = jwtUtil.getUsernameFromToken(state);
        socialAccountService.connectInstagramAccount(code, username);
        response.sendRedirect("http://localhost:3000/success");
    }

    @GetMapping("/callback/threads")
    public void handleThreadsCallback(
        @RequestParam(name = "code", required = false) String code,
        @RequestParam(name = "error", required = false) String error,
        @RequestParam(name = "state", required = false) String state,
        HttpServletResponse response
    ) throws Exception {
        if (error != null) {
            response.sendRedirect("http://localhost:3000/failed");
            return;
        }

        String username = jwtUtil.getUsernameFromToken(state);
        socialAccountService.connectThreadsAccount(code, username);
        response.sendRedirect("http://localhost:3000/success");
    }

    @GetMapping("/callback/tiktok")
    public void handleTikTokCallback(
        @RequestParam(name = "code", required = false) String code,
        @RequestParam(name = "error", required = false) String error,
        @RequestParam(name = "state", required = false) String state,
        HttpServletResponse response
    ) throws Exception {
        if (error != null) {
            response.sendRedirect("http://localhost:3000/failed");
            return;
        }

        if (state == null || !TikTokClient.pkceStorage.containsKey(state)) {
            // Có dấu hiệu tấn công CSRF hoặc session đã hết hạn
            System.out.println("LỖI: STATE KHÔNG KHỚP HOẶC BỊ NULL!");
            response.sendRedirect("http://localhost:3000/failed");
            return;
        }

        String codeVerifier = TikTokClient.pkceStorage.remove(state);
        String username = jwtUtil.getUsernameFromToken(state);


        socialAccountService.connectTikTokAccount(code, codeVerifier, username);
        response.sendRedirect("http://localhost:3000/success");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SocialAccountDto>>> getMySocialAccounts(Authentication authentication) {
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
        String username = authentication.getName();
        socialAccountService.deleteSocialAccountById(id, username);
        return ResponseEntity.ok(
            ApiResponse.ok("Social account deleted successfully")
        );
    }
}