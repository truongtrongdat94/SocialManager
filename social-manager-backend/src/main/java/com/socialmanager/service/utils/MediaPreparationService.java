package com.socialmanager.service.utils;

import com.socialmanager.exception.BusinessException;
import com.socialmanager.model.Platform;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
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
            .filter(candidate -> isSupportedMediaUrl(platform, candidate))
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

    private boolean isSupportedMediaUrl(Platform platform, String rawUrl) {
        String extension = extensionOf(rawUrl);
        if (supportsExtension(platform, extension)) {
            return true;
        }

        if (!extension.isBlank()) {
            return false;
        }

        String contentType = detectContentType(rawUrl);
        if (contentType == null || contentType.isBlank()) {
            return false;
        }

        String normalized = contentType.toLowerCase(Locale.ROOT);
        if (platform == Platform.TIKTOK) {
            return normalized.startsWith("video/");
        }

        return normalized.startsWith("image/") || normalized.startsWith("video/");
    }

    private String detectContentType(String rawUrl) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(rawUrl).openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);

            int status = connection.getResponseCode();
            if (status >= 400) {
                return null;
            }

            String contentType = connection.getContentType();
            if (contentType != null && !contentType.isBlank()) {
                return contentType;
            }

            return connection.getHeaderField("Content-Type");
        } catch (IOException ex) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
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