package com.socialmanager.client;

import com.socialmanager.dto.external.*;
import com.socialmanager.dto.external.TikTokResponse.TikTok;
import com.socialmanager.util.PKCEUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class TikTokClient {
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.tiktok.client-key:${TIKTOK_CLIENT_KEY:}}")
    private String tiktokClientKey;

    @Value("${app.tiktok.client-secret:${TIKTOK_CLIENT_SECRET:}}")
    private String tiktokClientSecret;

    @Value("${app.tiktok.redirect-uri:${TIKTOK_REDIRECT_URI:http://localhost:8080/api/social-accounts/callback/tiktok}}")
    private String tiktokRedirectUri;

    public static final Map<String, String> pkceStorage = new ConcurrentHashMap<>();

    public String getAuthUrl(String stateJwt) {
        String codeVerifier = PKCEUtil.generateCodeVerifier();
        String codeChallenge = PKCEUtil.generateCodeChallenge(codeVerifier);

        pkceStorage.put(stateJwt, codeVerifier);

        return String.format(
            "https://www.tiktok.com/v2/auth/authorize/" +
                "?disable_auto_auth=1" +
                "&client_key=%s" +
                "&redirect_uri=%s" +
                "&response_type=code" +
                "&scope=user.info.basic,video.publish" +
                "&state=%s" +
                "&code_challenge=%s" +
                "&code_challenge_method=S256",

            tiktokClientKey,
            URLEncoder.encode(tiktokRedirectUri, StandardCharsets.UTF_8),
            stateJwt,
            codeChallenge
        );
    }

    public TokenResponse exchangeCodeForTikTokAccessToken(String code, String codeVerifier) {
        String tokenUrl = "https://open.tiktokapis.com/v2/oauth/token/";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_key", tiktokClientKey);
        body.add("client_secret", tiktokClientSecret);
        body.add("code", code);
        body.add("grant_type", "authorization_code");
        body.add("redirect_uri", tiktokRedirectUri);
        body.add("code_verifier", codeVerifier);     // V2 bắt buộc truyền code_verifier để verify PKCE

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setCacheControl(CacheControl.noCache()); // V2 yêu cầu Cache-Control: no-cache cho api lấy token

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        TokenResponse response = restTemplate.postForObject(tokenUrl, request, TokenResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new RuntimeException("Failed to get TikTok access token");
        }

        return response;
    }

    public TikTok fetchTikTokAccount(String token) {
        String userInfoUrl = "https://open.tiktokapis.com/v2/user/info/?fields=open_id,union_id,avatar_url,display_name";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<TikTokResponse> response = restTemplate.exchange(
            userInfoUrl,
            HttpMethod.GET,
            request,
            TikTokResponse.class
        );

        TikTokResponse body = response.getBody();

        if (body == null || body.data() == null || body.data().user() == null) {
            throw new RuntimeException("Failed to fetch TikTok user info");
        }

        return body.data().user();
    }

    public TokenResponse refreshAccessToken(String currentRefreshToken) {
        String tokenUrl = "https://open.tiktokapis.com/v2/oauth/token/";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_key", tiktokClientKey);
        body.add("client_secret", tiktokClientSecret);
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", currentRefreshToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setCacheControl(CacheControl.noCache());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        TokenResponse response = restTemplate.postForObject(tokenUrl, request, TokenResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new RuntimeException("Failed to refresh TikTok access token. Refresh token might be expired.");
        }

        return response;
    }
}

