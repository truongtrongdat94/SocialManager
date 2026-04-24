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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetaSocialPostPublisherTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void retriesTransientFailuresAndSendsIdempotencyKey() throws Exception {
        AtomicInteger callCount = new AtomicInteger();
        List<String> capturedBodies = new ArrayList<>();
        List<String> capturedIdempotencyKeys = new ArrayList<>();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/123/feed", exchange -> handleMetaExchange(exchange, callCount, capturedBodies, capturedIdempotencyKeys));
        server.start();

        MetaSocialPostPublisher publisher = new MetaSocialPostPublisher(
                3,
                1,
                "http://localhost:" + server.getAddress().getPort()
        );

        String result = publisher.publish(new SocialPostPublishRequest(
                "FACEBOOK",
                "123",
                "token-abc",
                "seo caption",
                "https://cdn.example.com/image.png",
                "post-abc"
        ));

        assertEquals("meta-post-1", result);
        assertEquals(2, callCount.get());
        assertEquals(2, capturedBodies.size());
        assertEquals("post-abc", capturedIdempotencyKeys.get(0));
        assertTrue(capturedBodies.get(0).contains("seo caption"));
        assertTrue(capturedBodies.get(0).contains("image.png"));
    }

    private void handleMetaExchange(HttpExchange exchange, AtomicInteger callCount,
                                     List<String> capturedBodies,
                                     List<String> capturedIdempotencyKeys) throws IOException {
        callCount.incrementAndGet();
        capturedIdempotencyKeys.add(exchange.getRequestHeaders().getFirst("X-Idempotency-Key"));
        capturedBodies.add(readBody(exchange.getRequestBody()));

        if (callCount.get() == 1) {
            sendResponse(exchange, 500, "{\"error\":\"temporary\"}");
            return;
        }

        sendResponse(exchange, 200, "{\"id\":\"meta-post-1\"}");
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
