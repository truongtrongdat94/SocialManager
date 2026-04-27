package com.socialmanager.service.utils;

import com.socialmanager.exception.BusinessException;
import com.socialmanager.model.Platform;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

@Service
public class MediaPreparationService {

    private static final Set<String> META_ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif", "mp4", "mov", "webm");
    private static final Set<String> TIKTOK_ALLOWED_EXTENSIONS = Set.of("mp4", "mov", "webm");

    public String preparePrimaryMediaUrl(Platform platform, String[] mediaUrls) {
        if (mediaUrls == null || mediaUrls.length == 0) {
            return null;
        }

        return Arrays.stream(mediaUrls)
                .filter(candidate -> candidate != null && !candidate.isBlank())
                .map(String::trim)
                .filter(this::isHttpOrHttps)
                .filter(candidate -> supportsExtension(platform, extensionOf(candidate)))
                .findFirst()
                .orElseThrow(() -> new BusinessException("No supported media URL found for platform " + platform));
    }

    private boolean isHttpOrHttps(String rawUrl) {
        try {
            URI uri = URI.create(rawUrl);
            String scheme = uri.getScheme();
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean supportsExtension(Platform platform, String extension) {
        if (extension.isBlank()) {
            return false;
        }

        if (platform == Platform.TIKTOK) {
            return TIKTOK_ALLOWED_EXTENSIONS.contains(extension);
        }

        return META_ALLOWED_EXTENSIONS.contains(extension);
    }

    private String extensionOf(String rawUrl) {
        String lower = rawUrl.toLowerCase(Locale.ROOT);
        int querySeparator = lower.indexOf('?');
        if (querySeparator >= 0) {
            lower = lower.substring(0, querySeparator);
        }

        int dotIndex = lower.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == lower.length() - 1) {
            return "";
        }

        return lower.substring(dotIndex + 1);
    }
}