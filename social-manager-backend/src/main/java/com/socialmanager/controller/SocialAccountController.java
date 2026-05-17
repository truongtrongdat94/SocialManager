package com.socialmanager.controller;

import com.socialmanager.client.TikTokClient;
import com.socialmanager.dto.ApiResponse;
import com.socialmanager.dto.SocialAccountDto;
import com.socialmanager.model.Platform;
import com.socialmanager.service.account.SocialAccountService;
import com.socialmanager.util.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/social-accounts")
@RequiredArgsConstructor
public class SocialAccountController {
    private final JwtUtil jwtUtil;
    private final SocialAccountService socialAccountService;

    @Value("${app.frontend-url:http://localhost:3001}")
    private String frontendUrl;

    @Value("${app.facebook.client-id:${app.meta.app-id:${META_APP_ID:${FACEBOOK_CLIENT_ID:}}}}")
    private String facebookClientId;

    @Value("${app.facebook.client-secret:${app.meta.app-secret:${META_APP_SECRET:${FACEBOOK_CLIENT_SECRET:}}}}")
    private String facebookClientSecret;

    @Value("${app.instagram.client-id:${INSTAGRAM_CLIENT_ID:}}")
    private String instagramClientId;

    @Value("${app.instagram.client-secret:${INSTAGRAM_CLIENT_SECRET:}}")
    private String instagramClientSecret;

    @Value("${app.threads.client-id:${THREADS_CLIENT_ID:}}")
    private String threadsClientId;

    @Value("${app.threads.client-secret:${THREADS_CLIENT_SECRET:}}")
    private String threadsClientSecret;

    @Value("${app.tiktok.client-key:${TIKTOK_CLIENT_KEY:}}")
    private String tiktokClientKey;

    @Value("${app.tiktok.client-secret:${TIKTOK_CLIENT_SECRET:}}")
    import java.io.IOException;
    private String tiktokClientSecret;

    @Value("${app.cloudinary.cloud-name:${CLOUDINARY_CLOUD_NAME:}}")
    private String cloudinaryCloudName;

    @Value("${app.cloudinary.api-key:${CLOUDINARY_API_KEY:}}")
    private String cloudinaryApiKey;

        private void redirect(HttpServletResponse response) throws IOException {
            response.sendRedirect(frontendUrl + "/dashboard/accounts?status=success");
        }

    private void redirectFailed(HttpServletResponse response, String reason) throws Exception {
        String encodedReason = URLEncoder.encode(reason, StandardCharsets.UTF_8);
        response.sendRedirect(frontendPath("/failed?reason=" + encodedReason));
    }

    @GetMapping("/connect/{platform}")
    public ResponseEntity<ApiResponse<String>> getConnectUrl(@PathVariable Platform platform, Authentication authentication) {
        String username = authentication.getName();
        String url = socialAccountService.generateAuthUrl(platform, username);
        return ResponseEntity.ok(ApiResponse.ok(url));
    }

<<<<<<< HEAD
    @GetMapping("/config")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getPlatformConfig() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
            "facebook", hasValue(facebookClientId) && hasValue(facebookClientSecret),
            "instagram", hasValue(instagramClientId) && hasValue(instagramClientSecret),
            "threads", hasValue(threadsClientId) && hasValue(threadsClientSecret),
            redirect(response);
            "cloudinary", hasValue(cloudinaryCloudName) && hasValue(cloudinaryApiKey) && hasValue(cloudinaryApiSecret)
        )));
=======
    private void redirect(HttpServletResponse response) throws IOException {
        response.sendRedirect(frontendUrl + "/dashboard/accounts?status=success");
>>>>>>> 3445fc14962b3c9fae27fefc3d4a774127a5382e
    }

    @GetMapping("/callback/facebook")
    public void handleFacebookCallback(
        @RequestParam(name = "code", required = false) String code,
        @RequestParam(name = "error", required = false) String error,
        @RequestParam(name = "state", required = false) String state,
        HttpServletResponse response
    ) throws Exception {
        if (error != null) {
            redirectFailed(response, "Facebook returned an OAuth error: " + error);
            return;
        }

<<<<<<< HEAD
            redirect(response);
            redirectFailed(response, "Missing OAuth code/state in Facebook callback");
            return;
        }

        try {
            String username = jwtUtil.getUsernameFromToken(state);
            socialAccountService.connectFacebookAccount(code, username);
            response.sendRedirect(frontendPath("/success"));
        } catch (Exception ex) {
            redirectFailed(response, ex.getMessage() != null ? ex.getMessage() : "Facebook connection failed");
        }
=======
        String username = jwtUtil.getUsernameFromToken(state);
        socialAccountService.connectFacebookAccount(code, username);
        redirect(response);
>>>>>>> 3445fc14962b3c9fae27fefc3d4a774127a5382e
    }

    @GetMapping("/callback/instagram")
    public void handleInstagramCallback(
        @RequestParam(name = "code", required = false) String code,
            redirect(response);
        @RequestParam(name = "state", required = false) String state,
        HttpServletResponse response
    ) throws Exception {
        if (error != null) {
            response.sendRedirect(frontendPath("/failed"));
            return;
        }

        if (code == null || state == null) {
            response.sendRedirect(frontendPath("/failed"));
            return;
        }

        String username = jwtUtil.getUsernameFromToken(state);
        socialAccountService.connectInstagramAccount(code, username);
<<<<<<< HEAD
        response.sendRedirect(frontendPath("/success"));
=======
        redirect(response);
>>>>>>> 3445fc14962b3c9fae27fefc3d4a774127a5382e
    }

    @GetMapping("/callback/threads")
    public void handleThreadsCallback(
        @RequestParam(name = "code", required = false) String code,
            redirect(response);
        @RequestParam(name = "state", required = false) String state,
        HttpServletResponse response
    ) throws Exception {
        if (error != null) {
            response.sendRedirect(frontendPath("/failed"));
            return;
        }

        if (code == null || state == null) {
            response.sendRedirect(frontendPath("/failed"));
            return;
        }

        String username = jwtUtil.getUsernameFromToken(state);
        socialAccountService.connectThreadsAccount(code, username);
<<<<<<< HEAD
        response.sendRedirect(frontendPath("/success"));
=======
        redirect(response);
>>>>>>> 3445fc14962b3c9fae27fefc3d4a774127a5382e
    }

    @GetMapping("/callback/tiktok")
    public void handleTikTokCallback(
        @RequestParam(name = "code", required = false) String code,
        @RequestParam(name = "error", required = false) String error,
        @RequestParam(name = "state", required = false) String state,
        HttpServletResponse response
    ) throws Exception {
        if (error != null) {
            response.sendRedirect(frontendPath("/failed"));
            return;
        }

        if (state == null || !TikTokClient.pkceStorage.containsKey(state)) {
            // Có dấu hiệu tấn công CSRF hoặc session đã hết hạn
            System.out.println("LỖI: STATE KHÔNG KHỚP HOẶC BỊ NULL!");
            response.sendRedirect(frontendPath("/failed"));
            return;
        }

        String codeVerifier = TikTokClient.pkceStorage.remove(state);
        String username = jwtUtil.getUsernameFromToken(state);


        socialAccountService.connectTikTokAccount(code, codeVerifier, username);
<<<<<<< HEAD
        response.sendRedirect(frontendPath("/success"));
=======
        redirect(response);
>>>>>>> 3445fc14962b3c9fae27fefc3d4a774127a5382e
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

    @PostMapping("/dev/test")
    public ResponseEntity<ApiResponse<SocialAccountDto>> createTestSocialAccount(
        @RequestParam Platform platform,
        @RequestParam(required = false, defaultValue = "Test Account") String accountName,
        Authentication authentication
    ) {
        String username = authentication.getName();
        return ResponseEntity.ok(
            ApiResponse.ok(
                socialAccountService.createTestAccount(username, platform, accountName)
            )
        );
    }
}