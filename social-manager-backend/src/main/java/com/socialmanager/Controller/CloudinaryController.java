package com.socialmanager.controller;

import com.socialmanager.dto.ApiResponse;
import com.socialmanager.dto.CloudinaryCredentialsRequest;
import com.socialmanager.dto.CloudinaryUploadResponse;
import com.socialmanager.model.CloudinaryCredentials;
import com.socialmanager.model.User;
import com.socialmanager.repository.CloudinaryCredentialsRepository;
import com.socialmanager.repository.UserRepository;
import com.socialmanager.service.CloudinaryService;
import com.socialmanager.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/cloudinary")
@RequiredArgsConstructor
public class CloudinaryController {

    private final CloudinaryCredentialsRepository credentialsRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    @Value("${app.aes-secret:}")
    private String aesSecret;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveCredentials(@RequestBody CloudinaryCredentialsRequest req) {
        if (aesSecret == null || aesSecret.isBlank()) {
            return ResponseEntity.status(500).body(new ApiResponse<>(false, "Server not configured to store secrets", null));
        }

        try {
            String cloudName = req.getCloudName() != null ? req.getCloudName().trim() : "";
            String apiKey = req.getApiKey() != null ? req.getApiKey().trim() : "";
            String apiSecret = req.getApiSecret() != null ? req.getApiSecret().trim() : "";

            if (cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Thiếu cloud name / api key / api secret", null));
            }

            User user = getCurrentUser();
            cloudinaryService.validateCredentials(cloudName, apiKey, apiSecret);
            CloudinaryCredentials cred = credentialsRepository.findByUser(user).orElse(CloudinaryCredentials.builder().user(user).build());
            cred.setCloudName(cloudName);
            cred.setApiKey(EncryptionUtil.encrypt(apiKey, aesSecret));
            cred.setApiSecret(EncryptionUtil.encrypt(apiSecret, aesSecret));
            credentialsRepository.save(cred);

            return ResponseEntity.ok(new ApiResponse<>(true, "Cloudinary credentials saved", Map.of("cloudName", cloudName, "configured", true, "valid", true)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ApiResponse<>(false, "Failed to save credentials: " + e.getMessage(), null));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCredentials() {
        User user = getCurrentUser();
        var opt = credentialsRepository.findByUser(user);
        if (opt.isPresent()) {
            CloudinaryCredentials c = opt.get();
            // Credentials were already validated when saving.
            return ResponseEntity.ok(new ApiResponse<>(true, "OK", Map.of("cloudName", c.getCloudName(), "configured", true, "valid", true)));
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "OK", Map.of("configured", false, "valid", false)));
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<CloudinaryUploadResponse>> uploadFile(@RequestPart("file") MultipartFile file) {
        try {
            User user = getCurrentUser();
            var opt = credentialsRepository.findByUser(user);
            if (opt.isEmpty()) {
                return ResponseEntity.status(400).body(new ApiResponse<>(false, "No Cloudinary configured for user", null));
            }

            CloudinaryCredentials c = opt.get();
            try {
                String key = EncryptionUtil.decrypt(c.getApiKey(), aesSecret);
                String secret = EncryptionUtil.decrypt(c.getApiSecret(), aesSecret);
                CloudinaryUploadResponse resp = cloudinaryService.upload(file, c.getCloudName(), key, secret);
                return ResponseEntity.ok(new ApiResponse<>(true, "Uploaded", resp));
            } catch (Exception ex) {
                return ResponseEntity.status(500).body(new ApiResponse<>(false, "Upload failed: " + ex.getMessage(), null));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ApiResponse<>(false, "Upload error: " + e.getMessage(), null));
        }
    }
}
