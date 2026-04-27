package com.socialmanager.service.post;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.socialmanager.dto.SocialPostPublishRequest;
import com.socialmanager.model.Platform;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Locale;

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
        return isVideoUrl(request.mediaUrl())
                ? tiktokBaseUrl + "/post/publish/video/init/"
                : tiktokBaseUrl + "/post/publish/content/init/";
    }

    @Override
    protected String buildPayload(SocialPostPublishRequest request) throws IOException {
        ObjectNode payload = objectMapper().createObjectNode();
        ObjectNode postInfo = payload.putObject("post_info");
        ObjectNode sourceInfo = payload.putObject("source_info");

        if (isVideoUrl(request.mediaUrl())) {
            postInfo.put("title", request.content());
            postInfo.put("privacy_level", "PUBLIC_TO_EVERYONE");
            postInfo.put("disable_duet", false);
            postInfo.put("disable_comment", false);
            postInfo.put("disable_stitch", false);
            sourceInfo.put("source", "PULL_FROM_URL");
            sourceInfo.put("video_url", request.mediaUrl());
        } else {
            postInfo.put("title", request.content());
            postInfo.put("description", request.content());
            postInfo.put("privacy_level", "PUBLIC_TO_EVERYONE");
            postInfo.put("auto_add_music", true);
            sourceInfo.put("source", "PULL_FROM_URL");
            sourceInfo.put("photo_cover_index", 1);
            ArrayNode photoImages = sourceInfo.putArray("photo_images");
            if (request.mediaUrl() != null && !request.mediaUrl().isBlank()) {
                photoImages.add(request.mediaUrl());
            }
            payload.put("post_mode", "DIRECT_POST");
            payload.put("media_type", "PHOTO");
        }

        return payload.toString();
    }

    private boolean isVideoUrl(String mediaUrl) {
        if (mediaUrl == null || mediaUrl.isBlank()) {
            return false;
        }

        String lower = mediaUrl.toLowerCase(Locale.ROOT);
        int queryIndex = lower.indexOf('?');
        if (queryIndex >= 0) {
            lower = lower.substring(0, queryIndex);
        }

        return lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".webm");
    }
}