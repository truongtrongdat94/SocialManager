package com.socialmanager.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import org.springframework.web.multipart.MultipartFile;
import com.socialmanager.dto.CloudinaryUploadResponse;
import java.util.HashMap;

@Service
@Slf4j
public class CloudinaryService {

    @Value("${app.cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${app.cloudinary.api-key:}")
    private String apiKey;

    @Value("${app.cloudinary.api-secret:}")
    private String apiSecret;

    private Cloudinary cloudinary;

    @PostConstruct
    public void init() {
        cloudinary = new Cloudinary(ObjectUtils.asMap(
            "cloud_name", cloudName,
            "api_key", apiKey,
            "api_secret", apiSecret,
            "secure", true
        ));
    }

    public String upload(String imageUrl) {
        try {
            log.info(">>>> Đang đẩy ảnh từ Leonardo lên Cloudinary...");
            Map uploadResult = cloudinary.uploader().upload(imageUrl, ObjectUtils.asMap(
                "folder", "social_manager_images"
            ));
            
            String secureUrl = (String) uploadResult.get("secure_url");
            log.info("✅ Upload thành công! Link vĩnh viễn: {}", secureUrl);
            
            return secureUrl;
        } catch (Exception e) {
            log.error("❌ Lỗi upload Cloudinary: {}", e.getMessage());
            return null;
        }
    }

    public void validateCredentials(String cloudName, String apiKey, String apiSecret) {
        try {
            Cloudinary c = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
            ));

            c.api().ping(ObjectUtils.emptyMap());
        } catch (Exception e) {
            throw new IllegalStateException("Cloudinary credentials are invalid: " + e.getMessage(), e);
        }
    }

    public CloudinaryUploadResponse upload(MultipartFile file, String overrideCloudName, String overrideApiKey, String overrideApiSecret) {
        try {
            Cloudinary c = cloudinary;
            if (overrideCloudName != null && !overrideCloudName.isBlank()) {
                Map<String, Object> conf = new HashMap<>();
                conf.put("cloud_name", overrideCloudName);
                conf.put("api_key", overrideApiKey);
                conf.put("api_secret", overrideApiSecret);
                conf.put("secure", true);
                c = new Cloudinary(conf);
            }

            log.info(">>>> Upload file to Cloudinary via backend...");
            Map uploadResult = c.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", "social_manager_images"
            ));

            String secureUrl = (String) uploadResult.get("secure_url");
            Integer bytes = (Integer) uploadResult.get("bytes");
            log.info("✅ Upload thành công! Link vĩnh viễn: {} ({} bytes)", secureUrl, bytes);

            return new CloudinaryUploadResponse(secureUrl, bytes != null ? bytes.longValue() : null);
        } catch (Exception e) {
            log.error("❌ Lỗi upload Cloudinary (file): {}", e.getMessage());
            throw new IllegalStateException("Cloudinary upload failed: " + e.getMessage(), e);
        }
    }
}