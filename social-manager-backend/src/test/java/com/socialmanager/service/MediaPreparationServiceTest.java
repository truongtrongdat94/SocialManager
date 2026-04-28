package com.socialmanager.service;

import com.socialmanager.exception.BusinessException;
import com.socialmanager.model.Platform;
import com.socialmanager.service.utils.MediaPreparationService;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
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

    @Test
    void acceptsExtensionlessImageUrlsWhenContentTypeIsImage() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        try {
            server.createContext("/image", this::handleImageHead);
            server.start();

            String selected = mediaPreparationService.preparePrimaryMediaUrl(
                    Platform.FACEBOOK,
                    new String[]{"http://localhost:" + server.getAddress().getPort() + "/image?token=abc"}
            );

            assertEquals("http://localhost:" + server.getAddress().getPort() + "/image?token=abc", selected);
        } finally {
            server.stop(0);
        }
    }

    private void handleImageHead(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "image/jpeg");
        exchange.sendResponseHeaders(200, -1);
        exchange.close();
    }
}