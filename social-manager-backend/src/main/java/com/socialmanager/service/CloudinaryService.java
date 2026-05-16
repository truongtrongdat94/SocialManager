package com.socialmanager.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.socialmanager.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@ConditionalOnExpression("'${app.cloudinary.cloud-name:}'.length() > 0 && '${app.cloudinary.api-key:}'.length() > 0 && '${app.cloudinary.api-secret:}'.length() > 0")
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

    public Map<String, Object> uploadMultipart(MultipartFile file, String folder, String resourceType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Media file is required");
        }

        if (folder == null || folder.isBlank()) {
            folder = "social_manager_uploads";
        }

        String safeResourceType = resourceType == null || resourceType.isBlank() ? "auto" : resourceType;
        String publicId = UUID.randomUUID().toString();

        try {
            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "public_id", publicId,
                            "resource_type", safeResourceType,
                            "overwrite", false,
                            "unique_filename", false
                    )
            );

            return uploadResult;
        } catch (Exception e) {
            log.error("❌ Lỗi upload media lên Cloudinary: {}", e.getMessage());
            throw new BusinessException("Cannot upload media to Cloudinary: " + e.getMessage());
        }
    }
}