package com.socialmanager.client;

import com.socialmanager.dto.external.*;
import com.socialmanager.exception.ExternalApiCallException;
import com.socialmanager.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class ThreadsClient {
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${THREADS_CLIENT_ID:}")
    private String threadsClientId;

    @Value("${THREADS_CLIENT_SECRET:}")
    private String threadsClientSecret;

    @Value("${THREADS_REDIRECT_URI:}")
    private String threadsRedirectUri;

    public String getAuthUrl(String stateJwt) {
        return String.format(
            "https://threads.net/oauth/authorize" +
                "?force_reauth=true" + // Hiện chưa có cách để reauth
                "&client_id=%s" +
                "&redirect_uri=%s" +
                "&response_type=code" +
                "&scope=threads_basic%%2Cthreads_content_publish%%2Cthreads_manage_insights" +
                "&state=%s",
            threadsClientId,
            URLEncoder.encode(threadsRedirectUri, StandardCharsets.UTF_8),
            stateJwt
        );
    }

    private String getLongTokenUrl(TokenResponse shortTokenRes) {
        if (shortTokenRes == null || shortTokenRes.accessToken() == null) {
            throw new ExternalApiCallException("Không thể lấy short token từ Threads (Phản hồi rỗng)");
        }
        String shortLivedToken = shortTokenRes.accessToken();

        return "https://graph.threads.net/access_token"
            + "?grant_type=th_exchange_token"
            + "&client_secret=" + threadsClientSecret
            + "&access_token=" + shortLivedToken;
    }

    public TokenResponse exchangeCodeForThreadsLongToken(String code) {
        if (code.endsWith("#_")) {
            code = code.substring(0, code.length() - 2);
        }
        try {

            String shortTokenUrl = "https://graph.threads.net/oauth/access_token";

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", threadsClientId);
            body.add("client_secret", threadsClientSecret);
            body.add("grant_type", "authorization_code");
            body.add("redirect_uri", threadsRedirectUri);
            body.add("code", code);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            TokenResponse shortTokenRes = restTemplate.postForObject(shortTokenUrl, request, TokenResponse.class);

            String longTokenUrl = getLongTokenUrl(shortTokenRes);

            TokenResponse longTokenRes = restTemplate.getForObject(longTokenUrl, TokenResponse.class);

            if (longTokenRes == null || longTokenRes.accessToken() == null) {
                throw new ExternalApiCallException("Không thể lấy short token từ Threads (Phản hồi rỗng)");
            }

            return longTokenRes;
        } catch (HttpClientErrorException e) {
            throw new ExternalApiCallException("Lỗi từ Threads API khi đổi token: " + e.getResponseBodyAsString());
        }
    }

    public ThreadsResponse fetchThreadsAccount(String token) {
        String userInfoUrl = "https://graph.threads.net/v1.0/me"
            + "?fields=id,username,name,threads_profile_picture_url"
            + "&access_token=" + token;

        try {
            ThreadsResponse response = restTemplate.getForObject(userInfoUrl, ThreadsResponse.class);

            if (response == null || response.id() == null) {
                throw new ExternalApiCallException("Không thể lấy thông tin user từ Threads (Phản hồi rỗng)");
            }

            return response;
        } catch (HttpClientErrorException e) {
            throw new ExternalApiCallException("Lỗi từ Threads API khi lấy thông tin account: " + e.getResponseBodyAsString());
        }

    }

    public TokenResponse refreshAccessToken(String currentAccessToken) {
        String url = "https://graph.threads.net/refresh_access_token" +
            "?grant_type=th_refresh_token" +
            "&access_token=" + currentAccessToken;
        try {
            TokenResponse response = restTemplate.getForObject(url, TokenResponse.class);

            if (response == null || response.accessToken() == null) {
                throw new ExternalApiCallException("Không thể làm mới token Threads (Phản hồi rỗng)");
            }

            return response;
        } catch (HttpClientErrorException e) {
            throw new ExternalApiCallException("Lỗi từ Threads API khi làm mới token: " + e.getResponseBodyAsString());
        }
    }
}

