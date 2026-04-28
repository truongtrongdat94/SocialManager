package com.socialmanager.service;

import com.socialmanager.dto.SocialPostPublishRequest;
import com.socialmanager.model.Platform;
import com.socialmanager.service.post.PlatformApiService;
import com.socialmanager.service.post.SocialPostPublisher;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlatformApiServiceTest {

    @Test
    void routesToMatchingPublisher() {
        SocialPostPublisher metaPublisher = new StubPublisher(Platform.FACEBOOK, "meta-id");
        SocialPostPublisher tiktokPublisher = new StubPublisher(Platform.TIKTOK, "tiktok-id");

        PlatformApiService service = new PlatformApiService(List.of(metaPublisher, tiktokPublisher));

        String result = service.publishPost(
                "FACEBOOK",
                "account-1",
                "token-1",
                "caption",
                "https://example.com/media.jpg",
                "idempotency-1"
        );

        assertEquals("meta-id", result);
    }

    @Test
    void rejectsUnsupportedPlatform() {
        PlatformApiService service = new PlatformApiService(List.of());

        assertThrows(RuntimeException.class, () -> service.publishPost(
                "BLUESKY",
                "account-1",
                "token-1",
                "caption",
                "https://example.com/media.jpg",
                "idempotency-1"
        ));
    }

    private static class StubPublisher implements SocialPostPublisher {

        private final Platform supportedPlatform;
        private final String response;

        private StubPublisher(Platform supportedPlatform, String response) {
            this.supportedPlatform = supportedPlatform;
            this.response = response;
        }

        @Override
        public boolean supports(Platform platform) {
            return platform == supportedPlatform;
        }

        @Override
        public String publish(SocialPostPublishRequest request) {
            return response;
        }
    }
}
