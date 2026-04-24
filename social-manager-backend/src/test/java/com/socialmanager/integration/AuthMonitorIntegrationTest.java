package com.socialmanager.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthMonitorIntegrationTest {

        private final ObjectMapper objectMapper = new ObjectMapper();

        @Value("${local.server.port}")
        private int port;

    @Test
    void loginAndMonitorEndpointsShouldReturnOk() {
                String loginBody = "{\"username\":\"devuser\",\"password\":\"devpass123\"}";
                String loginResponse = send("/api/auth/login", "POST", loginBody, null).body();

                JsonNode loginJson = readJson(loginResponse);
                String token = loginJson.path("token").asText();
        assertThat(token).isNotBlank();

                HttpResponse<String> summaryResponse = send("/api/posts/monitor/summary", "GET", null, token);
                assertThat(summaryResponse.statusCode()).isEqualTo(200);

                HttpResponse<String> recentResponse = send("/api/posts/monitor/recent", "GET", null, token);
                assertThat(recentResponse.statusCode()).isEqualTo(200);
        }

        private HttpResponse<String> send(String path, String method, String body, String token) {
                try {
                        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                                        .uri(URI.create("http://localhost:" + port + path));

                        if (token != null && !token.isBlank()) {
                                requestBuilder.header("Authorization", "Bearer " + token);
                        }

                        if ("POST".equalsIgnoreCase(method)) {
                                requestBuilder.header("Content-Type", "application/json");
                                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body, StandardCharsets.UTF_8));
                        } else {
                                requestBuilder.GET();
                        }

                        return HttpClient.newHttpClient().send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
                } catch (Exception ex) {
                        throw new RuntimeException("Request failed for path " + path, ex);
                }
        }

        private JsonNode readJson(String rawJson) {
                try {
                        return objectMapper.readTree(rawJson);
                } catch (Exception ex) {
                        throw new RuntimeException("Invalid JSON response", ex);
                }
    }
}