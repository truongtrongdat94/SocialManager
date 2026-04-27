package com.socialmanager.service.post;

import com.socialmanager.dto.SocialPostPublishRequest;
import com.socialmanager.exception.BusinessException;
import com.socialmanager.model.Platform;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Locale;

@Service
@Profile("!local")
public class MetaSocialPostPublisher extends AbstractHttpSocialPostPublisher {

    private final String metaGraphBaseUrl;

    public MetaSocialPostPublisher(
            @Value("${app.posting.max-retries:3}") int maxRetries,
            @Value("${app.posting.retry-backoff-ms:1000}") long retryBackoffMs,
            @Value("${app.meta.graph-base-url:https://graph.facebook.com/v20.0}") String metaGraphBaseUrl) {
        super(maxRetries, retryBackoffMs);
        this.metaGraphBaseUrl = metaGraphBaseUrl;
    }

    @Override
    public boolean supports(Platform platform) {
        return platform == Platform.FACEBOOK || platform == Platform.INSTAGRAM || platform == Platform.THREADS;
    }

    @Override
    protected Platform supportedPlatform() {
        return Platform.FACEBOOK;
    }

    @Override
    protected String resolveEndpoint(SocialPostPublishRequest request) {
        if (isVideoUrl(request.mediaUrl())) {
            throw new BusinessException("Facebook video publishing is not wired yet");
        }

        return hasImageUrl(request.mediaUrl())
                ? metaGraphBaseUrl + "/" + request.accountId() + "/photos"
                : metaGraphBaseUrl + "/" + request.accountId() + "/feed";
    }

    @Override
    protected String buildPayload(SocialPostPublishRequest request) throws IOException {
        var payload = objectMapper().createObjectNode();

        if (hasImageUrl(request.mediaUrl())) {
            payload.put("url", request.mediaUrl());
            payload.put("caption", request.content());
        } else {
            payload.put("message", request.content());
        }

        return payload.toString();
    }

    private boolean hasImageUrl(String mediaUrl) {
        return extensionOf(mediaUrl).equals("jpg")
                || extensionOf(mediaUrl).equals("jpeg")
                || extensionOf(mediaUrl).equals("png")
                || extensionOf(mediaUrl).equals("webp")
                || extensionOf(mediaUrl).equals("gif");
    }

    private boolean isVideoUrl(String mediaUrl) {
        String extension = extensionOf(mediaUrl);
        return extension.equals("mp4") || extension.equals("mov") || extension.equals("webm");
    }

    private String extensionOf(String mediaUrl) {
        if (mediaUrl == null || mediaUrl.isBlank()) {
            return "";
        }

        String lower = mediaUrl.toLowerCase(Locale.ROOT);
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