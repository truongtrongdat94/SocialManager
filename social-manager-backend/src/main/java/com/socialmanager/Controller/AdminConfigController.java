package com.socialmanager.controller;

import com.socialmanager.dto.request.MetaConfigRequest;
import com.socialmanager.dto.ApiResponse;
import com.socialmanager.service.ConfigService;
import com.socialmanager.client.FacebookClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
public class AdminConfigController {
    private final ConfigService configService;
    private final FacebookClient facebookClient;

    @PostMapping("/meta")
    public ResponseEntity<ApiResponse<String>> saveMetaConfig(@RequestBody MetaConfigRequest req, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthenticated"));
        }

        configService.setMetaConfig(req.getAppId(), req.getAppSecret(), req.getRedirectUri());
        return ResponseEntity.ok(ApiResponse.ok("Saved"));
    }

    @GetMapping("/meta")
    public ResponseEntity<ApiResponse<Object>> getMetaConfig(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthenticated"));
        }

        var masked = new java.util.HashMap<String, Object>();
        masked.put("appId", configService.getMetaAppId().orElse(null));
        masked.put("redirectUri", configService.getMetaRedirectUri().orElse(null));
        masked.put("hasSecret", configService.getMetaAppSecretDecrypted().isPresent());

        return ResponseEntity.ok(ApiResponse.ok(masked));
    }

    @PostMapping("/meta/test")
    public ResponseEntity<ApiResponse<String>> testMetaConfig(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthenticated"));
        }

        try {
            // Use FacebookClient to generate the auth URL which validates config
            String url = facebookClient.getAuthUrl("test-state-jwt");
            return ResponseEntity.ok(ApiResponse.ok(url));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
        }
    }
}
