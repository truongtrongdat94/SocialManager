package com.socialmanager.client;

import com.socialmanager.dto.external.*;
import com.socialmanager.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class InstagramClient {
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${INSTAGRAM_CLIENT_ID}")
    private String instagramClientId;

    @Value("${INSTAGRAM_CLIENT_SECRET}")
    private String instagramClientSecret;

    @Value("${INSTAGRAM_REDIRECT_URI}")
    private String instagramRedirectUri;

    public String getAuthUrl(String stateJwt) {
        return String.format(
            "https://www.instagram.com/oauth/authorize" +
                "?force_reauth=true" +
                "&client_id=%s" +
                "&redirect_uri=%s" +
                "&response_type=code" +
                "&scope=instagram_business_basic,instagram_business_manage_messages,instagram_business_manage_comments,instagram_business_content_publish,instagram_business_manage_insights" +
                "&state=%s",
            instagramClientId,
            URLEncoder.encode(instagramRedirectUri, StandardCharsets.UTF_8),
            stateJwt
        );
    }

    public TokenResponse exchangeCodeForInstagramLongToken(String code) {
        if (code.endsWith("#_")) {
            code = code.substring(0, code.length() - 2);
        }

        String shortTokenUrl = "https://api.instagram.com/oauth/access_token";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", instagramClientId);
        body.add("client_secret", instagramClientSecret);
        body.add("grant_type", "authorization_code");
        body.add("redirect_uri", instagramRedirectUri);
        body.add("code", code);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        TokenResponse shortTokenRes = restTemplate.postForObject(shortTokenUrl, request, TokenResponse.class);

        if (shortTokenRes == null || shortTokenRes.accessToken() == null) {
            throw new RuntimeException("Failed to get Instagram short-lived token");
        }

        String shortLivedToken = shortTokenRes.accessToken();

        String longTokenUrl = "https://graph.instagram.com/access_token"
            + "?grant_type=ig_exchange_token"
            + "&client_secret=" + instagramClientSecret
            + "&access_token=" + shortLivedToken;

        TokenResponse longTokenRes = restTemplate.getForObject(longTokenUrl, TokenResponse.class);

        if (longTokenRes == null || longTokenRes.accessToken() == null) {
            throw new RuntimeException("Failed to get Instagram long-lived token");
        }

        return longTokenRes;
    }

    public InstagramResponse fetchInstagramAccount(String token) {
        String userInfoUrl = "https://graph.instagram.com/v25.0/me"
            + "?fields=id,username,name,profile_picture_url"
            + "&access_token=" + token;

        InstagramResponse response = restTemplate.getForObject(userInfoUrl, InstagramResponse.class);
        if (response == null || response.id() == null) {
            throw new RuntimeException("Failed to fetch Instagram user info");
        }

        return response;
    }

    public TokenResponse refreshAccessToken(String currentAccessToken) {
        String url = "https://graph.instagram.com/refresh_access_token" +
            "?grant_type=ig_refresh_token" +
            "&access_token=" + currentAccessToken;

        TokenResponse response = restTemplate.getForObject(url, TokenResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new RuntimeException("Failed to refresh Instagram access token. Token might be invalid or expired.");
        }

        return response;
    }
}

