package com.socialmanager.client;

import com.socialmanager.dto.external.*;
import com.socialmanager.dto.external.TikTokResponse.TikTok;
import com.socialmanager.exception.ExternalApiCallException;
import com.socialmanager.util.PKCEUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class TikTokClient {
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${TIKTOK_CLIENT_KEY:}")
    private String tiktokClientKey;

    @Value("${TIKTOK_CLIENT_SECRET:}")
    private String tiktokClientSecret;

    @Value("${TIKTOK_REDIRECT_URI:}")
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
                "&scope=user.info.basic" +
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
        body.add("code_verifier", codeVerifier);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setCacheControl(CacheControl.noCache());
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            TokenResponse response = restTemplate.postForObject(tokenUrl, request, TokenResponse.class);
            if (response == null || response.accessToken() == null) {
                throw new ExternalApiCallException("Không thể lấy access token từ TikTok (Phản hồi rỗng)");
            }
            return response;
        } catch (HttpClientErrorException e) {
            throw new ExternalApiCallException("Lỗi từ TikTok API khi đổi token: " + e.getResponseBodyAsString());
        }
    }

    public TikTok fetchTikTokAccount(String token) {
        String userInfoUrl = "https://open.tiktokapis.com/v2/user/info/?fields=open_id,union_id,avatar_url,display_name";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<TikTokResponse> response = restTemplate.exchange(
                userInfoUrl,
                HttpMethod.GET,
                request,
                TikTokResponse.class
            );

            TikTokResponse body = response.getBody();

            if (body == null || body.data() == null || body.data().user() == null) {
                throw new ExternalApiCallException("Không thể lấy thông tin user từ TikTok (Phản hồi rỗng)");
            }

            return body.data().user();
        } catch (HttpClientErrorException e) {
            throw new ExternalApiCallException("Lỗi từ TikTok API khi lấy thông tin account: " + e.getResponseBodyAsString());
        }

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

        try {
            TokenResponse response = restTemplate.postForObject(tokenUrl, request, TokenResponse.class);
            if (response == null || response.accessToken() == null) {
                throw new ExternalApiCallException("Không thể làm mới token TikTok (Phản hồi rỗng)");
            }
            return response;
        } catch (HttpClientErrorException e) {
            throw new ExternalApiCallException("Lỗi từ TikTok API khi làm mới token: " + e.getResponseBodyAsString());
        }
    }
}

