package com.socialmanager.dto;

public record MediaUploadResponse(
        String secureUrl,
        String publicId,
        String resourceType,
        String originalFilename,
        long sizeBytes
) {
}