package com.socialmanager.service.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmanager.dto.SocialPostPublishRequest;
import com.socialmanager.exception.BusinessException;
import com.socialmanager.model.Platform;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

abstract class AbstractHttpSocialPostPublisher implements SocialPostPublisher {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final int maxRetries;
    private final long retryBackoffMs;

    protected AbstractHttpSocialPostPublisher(int maxRetries, long retryBackoffMs) {
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.maxRetries = maxRetries;
        this.retryBackoffMs = retryBackoffMs;
    }

    @Override
    public final String publish(SocialPostPublishRequest request) {
        validateRequest(request);
        return executeWithRetry(request);
    }

    protected abstract Platform supportedPlatform();

    protected abstract String resolveEndpoint(SocialPostPublishRequest request);

    protected abstract String buildPayload(SocialPostPublishRequest request) throws IOException;

    protected String contentType(SocialPostPublishRequest request) {
        return "application/json";
    }

    protected String authorizationHeaderValue(SocialPostPublishRequest request) {
        return "Bearer " + request.token();
    }

    protected ObjectMapper objectMapper() {
        return objectMapper;
    }

    protected String extractPlatformPostId(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode idNode = root.path("id");
            if (!idNode.isMissingNode() && !idNode.asText().isBlank()) {
                return idNode.asText();
            }
            JsonNode publishIdNode = root.path("data").path("publish_id");
            if (!publishIdNode.isMissingNode() && !publishIdNode.asText().isBlank()) {
                return publishIdNode.asText();
            }
            JsonNode postIdNode = root.path("data").path("post_id");
            if (!postIdNode.isMissingNode() && !postIdNode.asText().isBlank()) {
                return postIdNode.asText();
            }
            JsonNode dataIdNode = root.path("data").path("id");
            if (!dataIdNode.isMissingNode() && !dataIdNode.asText().isBlank()) {
                return dataIdNode.asText();
            }
        } catch (Exception ex) {
            throw new BusinessException("Unable to parse publish response: " + ex.getMessage());
        }

        throw new BusinessException("Publish response did not include post id");
    }

    private String executeWithRetry(SocialPostPublishRequest request) {
        RuntimeException lastError = null;

        for (int attempt = 1; attempt <= Math.max(1, maxRetries); attempt++) {
            try {
                HttpResult result = publishOnce(request);
                if (result.statusCode() >= 200 && result.statusCode() < 300) {
                    return extractPlatformPostId(result.body());
                }

                if (!isTransientStatus(result.statusCode())) {
                    throw new BusinessException("Platform publish rejected with HTTP " + result.statusCode());
                }

                lastError = new BusinessException("Transient publish failure HTTP " + result.statusCode());
            } catch (IOException | InterruptedException ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    throw new BusinessException("Publishing interrupted");
                }
                lastError = new BusinessException("Publish request failed: " + ex.getMessage());
            }

            if (attempt < Math.max(1, maxRetries)) {
                sleepBackoff(attempt);
            }
        }

        throw lastError != null ? lastError : new BusinessException("Publish failed without concrete error");
    }

    private HttpResult publishOnce(SocialPostPublishRequest request) throws IOException, InterruptedException {
        String endpoint = resolveEndpoint(request);
        String payload = buildPayload(request);
        String contentType = contentType(request);

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(20))
            .header("Content-Type", contentType)
            .header("X-Idempotency-Key", request.idempotencyKey());

        String authorizationHeader = authorizationHeaderValue(request);
        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            builder.header("Authorization", authorizationHeader);
        }

        HttpRequest httpRequest = builder
            .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
            .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        return new HttpResult(response.statusCode(), response.body());
    }

    private boolean isTransientStatus(int statusCode) {
        return statusCode == 429 || statusCode >= 500;
    }

    private void sleepBackoff(int attempt) {
        try {
            Thread.sleep(retryBackoffMs * attempt);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Publishing retry interrupted");
        }
    }

    private void validateRequest(SocialPostPublishRequest request) {
        if (request == null) {
            throw new BusinessException("Publish request must not be null");
        }
        if (request.token() == null || request.token().isBlank()) {
            throw new BusinessException("Missing access token for platform publishing");
        }
        if (request.accountId() == null || request.accountId().isBlank()) {
            throw new BusinessException("Missing account id for platform publishing");
        }
    }

    private record HttpResult(int statusCode, String body) {
    }
}