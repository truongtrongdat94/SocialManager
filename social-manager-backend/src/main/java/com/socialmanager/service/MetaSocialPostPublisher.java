package com.socialmanager.service;

import com.socialmanager.model.Platform;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

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
        return metaGraphBaseUrl + "/" + request.accountId() + "/feed";
    }

    @Override
    protected String buildPayload(SocialPostPublishRequest request) throws IOException {
        var payload = objectMapper().createObjectNode()
                .put("message", request.content());

        if (request.mediaUrl() != null && !request.mediaUrl().isBlank()) {
            payload.put("media_url", request.mediaUrl());
        }

        return payload.toString();
    }
}
