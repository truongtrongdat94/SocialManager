package com.socialmanager.service;

import com.socialmanager.exception.BusinessException;
import com.socialmanager.model.Platform;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlatformApiService {

    private final List<SocialPostPublisher> publishers;

    public String publishPost(String platform, String accountId, String token,
                              String content, String mediaUrl, String idempotencyKey) {
        Platform targetPlatform;
        try {
            targetPlatform = Platform.valueOf(platform.toUpperCase());
        } catch (Exception ex) {
            throw new BusinessException("Unsupported platform: " + platform);
        }

        SocialPostPublisher publisher = publishers.stream()
                .filter(candidate -> candidate.supports(targetPlatform))
                .findFirst()
                .orElseThrow(() -> new BusinessException("No publisher configured for platform: " + platform));

        return publisher.publish(new SocialPostPublishRequest(
                targetPlatform.name(),
                accountId,
                token,
                content,
                mediaUrl,
                idempotencyKey
        ));
    }
}