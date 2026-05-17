package com.socialmanager.service.media;

import com.socialmanager.dto.MediaUploadResponse;
import com.socialmanager.exception.BusinessException;
import com.socialmanager.model.User;
import com.socialmanager.service.CloudinaryService;
import com.socialmanager.service.account.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MediaUploadService {

    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final long MAX_VIDEO_BYTES = 150L * 1024 * 1024;

    private final ObjectProvider<CloudinaryService> cloudinaryServiceProvider;
    private final CurrentUserService currentUserService;

    public MediaUploadResponse upload(MultipartFile file) {
        User currentUser = currentUserService.getCurrentUser();
        CloudinaryService cloudinaryService = cloudinaryServiceProvider.getIfAvailable();
        if (cloudinaryService == null) {
            throw new BusinessException("Media upload is not configured on this environment");
        }

        validateFile(file);

        String resourceType = resolveResourceType(file);
        String folder = "social_manager_uploads/" + currentUser.getId();

        Map<String, Object> result = cloudinaryService.uploadMultipart(file, folder, resourceType);

        String secureUrl = stringValue(result.get("secure_url"));
        String publicId = stringValue(result.get("public_id"));
        String returnedResourceType = stringValue(result.get("resource_type"));

        if (secureUrl == null || secureUrl.isBlank()) {
            throw new BusinessException("Cloudinary did not return a secure URL");
        }

        return new MediaUploadResponse(
                secureUrl,
                publicId,
                returnedResourceType == null ? resourceType : returnedResourceType,
                file.getOriginalFilename(),
                file.getSize()
        );
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Media file is required");
        }

        long size = file.getSize();
        String resourceType = resolveResourceType(file);
        if ("video".equals(resourceType) && size > MAX_VIDEO_BYTES) {
            throw new BusinessException("Video files must be 150MB or smaller");
        }

        if ("image".equals(resourceType) && size > MAX_IMAGE_BYTES) {
            throw new BusinessException("Image files must be 10MB or smaller");
        }
    }

    private String resolveResourceType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null) {
            String normalized = contentType.toLowerCase(Locale.ROOT);
            if (normalized.startsWith("video/")) {
                return "video";
            }
            if (normalized.startsWith("image/")) {
                return "image";
            }
        }

        String filename = file.getOriginalFilename();
        if (filename != null) {
            String lower = filename.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".webm")) {
                return "video";
            }
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".gif")) {
                return "image";
            }
        }

        throw new BusinessException("Unsupported media type. Please upload an image or video file.");
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}