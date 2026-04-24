package com.socialmanager.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TikTokSocialPostPublisherTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void publishesWithExpectedTikTokPayload() throws Exception {
        AtomicReference<String> bodyRef = new AtomicReference<>();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v2/post/publish", exchange -> {
            bodyRef.set(readBody(exchange.getRequestBody()));
            sendResponse(exchange, 200, "{\"data\":{\"id\":\"tiktok-post-9\"}}");
        });
        server.start();

        TikTokSocialPostPublisher publisher = new TikTokSocialPostPublisher(
                1,
                1,
                "http://localhost:" + server.getAddress().getPort() + "/v2"
        );

        String result = publisher.publish(new SocialPostPublishRequest(
                "TIKTOK",
                "acct-9",
                "token-xyz",
                "short caption",
                "https://cdn.example.com/video.mp4",
                "idempotency-9"
        ));

        assertEquals("tiktok-post-9", result);
        assertTrue(bodyRef.get().contains("\"account_id\":\"acct-9\""));
        assertTrue(bodyRef.get().contains("\"caption\":\"short caption\""));
    }

    private String readBody(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}
