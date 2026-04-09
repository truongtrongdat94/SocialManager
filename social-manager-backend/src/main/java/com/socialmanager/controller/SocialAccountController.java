package com.socialmanager.controller;

import com.socialmanager.dto.ApiResponse;
import com.socialmanager.model.Platform;
import com.socialmanager.service.SocialAccountService;
import com.socialmanager.util.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/social-accounts")
@RequiredArgsConstructor
public class SocialAccountController {
    private final JwtUtil jwtUtil;
    private final SocialAccountService socialAccountService;

    // 1. Lấy link để user nhảy sang Facebook/TikTok đăng nhập
    @GetMapping("/connect/{platform}")
    public ResponseEntity<ApiResponse<String>> getConnectUrl(@PathVariable Platform platform, Authentication authentication) {
        String username = authentication.getName();
        String url = socialAccountService.generateAuthUrl(platform, username);
        return ResponseEntity.ok(ApiResponse.ok(url));
    }

    // 2. Tiếp nhận "code" trả về từ Facebook/TikTok sau khi user đồng ý
    @GetMapping("/callback/meta")
    public void handleMetaCallback(
        @RequestParam(name = "code", required = false) String code,
        @RequestParam(name = "error", required = false) String error,
        HttpServletResponse response,
        @RequestParam(name = "state", required = false) String state
    ) throws Exception {
        String username = jwtUtil.getUsernameFromToken(state);
        if (error != null) {
            response.sendRedirect("http://localhost:3000/failed");
            return;
        }

        socialAccountService.connectMetaAccount(code, username);
        response.sendRedirect("http://localhost:3000/success");
    }

    @GetMapping("/callback/tiktok")
    public void handleTikTokCallback(
        @RequestParam(name = "code", required = false) String code,
        @RequestParam(name = "state", required = false) String state, // Thêm state
        @RequestParam(name = "error", required = false) String error,
        HttpServletResponse response
    ) throws Exception {
        if (error != null) {
            response.sendRedirect("http://localhost:3000/failed");
            return;
        }

        // Kiểm tra bảo mật (Xác thực state)
        if (state == null || !SocialAccountService.pkceStorage.containsKey(state)) {
            // Có dấu hiệu tấn công CSRF hoặc session đã hết hạn
            System.out.println(">>> LỖI: STATE KHÔNG KHỚP HOẶC BỊ NULL! <<<");
            response.sendRedirect("http://localhost:3000/failed");
            return;
        }

        String codeVerifier = SocialAccountService.pkceStorage.remove(state);
        String username = jwtUtil.getUsernameFromToken(state);

        try {
            socialAccountService.connectTikTokAccount(code, codeVerifier, username);
            response.sendRedirect("http://localhost:3000/success");
        } catch (Exception e) {response.sendRedirect("http://localhost:3000/login");
        }
    }
}