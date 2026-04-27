package com.socialmanager.service;

import com.socialmanager.exception.BusinessException;
import com.socialmanager.model.Platform;
import com.socialmanager.service.utils.MediaPreparationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MediaPreparationServiceTest {

    private final MediaPreparationService mediaPreparationService = new MediaPreparationService();

    @Test
    void returnsNullWhenNoMediaProvided() {
        assertNull(mediaPreparationService.preparePrimaryMediaUrl(Platform.FACEBOOK, null));
        assertNull(mediaPreparationService.preparePrimaryMediaUrl(Platform.FACEBOOK, new String[0]));
    }

    @Test
    void picksFirstSupportedMetaMediaUrl() {
        String selected = mediaPreparationService.preparePrimaryMediaUrl(
                Platform.FACEBOOK,
                new String[]{"https://example.com/file.txt", "https://example.com/photo.jpg"}
        );

        assertEquals("https://example.com/photo.jpg", selected);
    }

    @Test
    void rejectsUnsupportedTikTokMediaTypes() {
        assertThrows(BusinessException.class, () -> mediaPreparationService.preparePrimaryMediaUrl(
                Platform.TIKTOK,
                new String[]{"https://example.com/photo.jpg"}
        ));
    }

    @Test
    void acceptsTikTokVideoMediaTypes() {
        String selected = mediaPreparationService.preparePrimaryMediaUrl(
                Platform.TIKTOK,
                new String[]{"https://example.com/video.mp4"}
        );

        assertEquals("https://example.com/video.mp4", selected);
    }
}