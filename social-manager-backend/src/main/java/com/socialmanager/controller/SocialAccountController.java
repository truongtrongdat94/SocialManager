package com.socialmanager.controller;

import com.socialmanager.dto.ApiResponse;
import com.socialmanager.model.Platform;
import com.socialmanager.service.SocialAccountService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/social-accounts")
@RequiredArgsConstructor
public class SocialAccountController {

    private final SocialAccountService socialAccountService;

    // 1. Lấy link để user nhảy sang Facebook/TikTok đăng nhập
    @GetMapping("/connect/{platform}")
    public ResponseEntity<ApiResponse<String>> getConnectUrl(@PathVariable Platform platform) {
        String url = socialAccountService.generateAuthUrl(platform);
        return ResponseEntity.ok(ApiResponse.ok(url));
    }

    // 2. Tiếp nhận "code" trả về từ Facebook/TikTok sau khi user đồng ý
    @GetMapping("/callback/meta")
    public void handleMetaCallback(
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "error", required = false) String error,
            HttpServletResponse response,
            Authentication authentication
    ) throws Exception {
        if (error != null) {
            response.sendRedirect("http://localhost:3000");
            return;
        }

        String username = authentication != null ? authentication.getName() : "test";

        socialAccountService.connectMetaAccount(code, username);

        // redirect về FE sau khi connect xong
        String redirectUrl = "http://localhost:3000";
        response.sendRedirect(redirectUrl);
    }

    @GetMapping("/callback/tiktok")
    public void handleTikTokCallback(
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "state", required = false) String state, // Thêm state
            @RequestParam(name = "error", required = false) String error,
            HttpServletResponse response,
            Authentication authentication
    ) throws Exception {
        System.out.println("=== BẮT ĐẦU VÀO CALLBACK ===");
        // 1. Xử lý nếu người dùng từ chối cấp quyền hoặc có lỗi
        if (error != null) {
            response.sendRedirect("http://localhost:3000?error=access_denied");
            return;
        }


        // 3. Kiểm tra bảo mật (Xác thực state)
        if (state == null || !SocialAccountService.pkceStorage.containsKey(state)) {
            // Có dấu hiệu tấn công CSRF hoặc session đã hết hạn
            System.out.println(">>> LỖI: STATE KHÔNG KHỚP HOẶC BỊ NULL! <<<");
            response.sendRedirect("http://localhost:3000?error=invalid_state");
            return;
        }

        String codeVerifier = SocialAccountService.pkceStorage.remove(state);

        System.out.println(">>> STATE HỢP LỆ, CHUẨN BỊ GỌI SERVICE <<<");

        String username = authentication != null ? authentication.getName() : "test";

        try {
            // 4. Sửa lại tên hàm thành connectTikTokAccount và truyền thêm codeVerifier
            socialAccountService.connectTikTokAccount(code, codeVerifier, username);
            System.out.println("SUCCESS");
            // Thành công: Redirect về frontend kèm cờ success để frontend hiện thông báo
            response.sendRedirect("http://localhost:3000/success");
        } catch (Exception e) {response.sendRedirect("http://localhost:3000/login");
        }
    }
}