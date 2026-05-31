package com.socialmanager.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class CloudinaryService {

    @Value("${app.cloudinary.cloud-name}")
    private String cloudName;

    @Value("${app.cloudinary.api-key}")
    private String apiKey;

    @Value("${app.cloudinary.api-secret}")
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
}