package com.socialmanager.service;

import com.socialmanager.model.Platform;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Profile("!local")
public class TikTokSocialPostPublisher extends AbstractHttpSocialPostPublisher {

    private final String tiktokBaseUrl;

    public TikTokSocialPostPublisher(
            @Value("${app.posting.max-retries:3}") int maxRetries,
            @Value("${app.posting.retry-backoff-ms:1000}") long retryBackoffMs,
            @Value("${app.tiktok.api-base-url:https://open.tiktokapis.com/v2}") String tiktokBaseUrl) {
        super(maxRetries, retryBackoffMs);
        this.tiktokBaseUrl = tiktokBaseUrl;
    }

    @Override
    public boolean supports(Platform platform) {
        return platform == Platform.TIKTOK;
    }

    @Override
    protected Platform supportedPlatform() {
        return Platform.TIKTOK;
    }

    @Override
    protected String resolveEndpoint(SocialPostPublishRequest request) {
        return tiktokBaseUrl + "/post/publish";
    }

    @Override
    protected String buildPayload(SocialPostPublishRequest request) throws IOException {
        var payload = objectMapper().createObjectNode()
                .put("account_id", request.accountId())
                .put("caption", request.content());

        if (request.mediaUrl() != null && !request.mediaUrl().isBlank()) {
            payload.put("media_url", request.mediaUrl());
        }

        return payload.toString();
    }
}
