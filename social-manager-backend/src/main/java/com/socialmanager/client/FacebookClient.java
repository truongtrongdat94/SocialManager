package com.socialmanager.client;

import com.socialmanager.dto.external.*;
import com.socialmanager.dto.external.FacebookResponse.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FacebookClient {
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${FACEBOOK_CLIENT_ID}")
    private String facebookClientId;

    @Value("${FACEBOOK_CLIENT_SECRET}")
    private String facebookClientSecret;

    @Value("${FACEBOOK_REDIRECT_URI}")
    private String facebookRedirectUri;

    public String getAuthUrl(String stateJwt) {
        return String.format(
            "https://www.facebook.com/v25.0/dialog/oauth" +
                "?client_id=%s" +
                "&redirect_uri=%s" +
                "&response_type=code" +
                "&scope=pages_manage_metadata,pages_manage_posts,pages_manage_engagement,pages_read_engagement,pages_read_user_engagement,pages_show_list,publish_video" +
                "&state=%s",
            facebookClientId,
            URLEncoder.encode(facebookRedirectUri, StandardCharsets.UTF_8),
            stateJwt
        );
    }

    public TokenResponse exchangeCodeForFacebookLongToken(String code) {
        String shortTokenUrl = "https://graph.facebook.com/oauth/access_token" +
            "?client_id=" + facebookClientId +
            "&client_secret=" + facebookClientSecret +
            "&redirect_uri=" + facebookRedirectUri +
            "&code=" + code;

        TokenResponse shortRes = restTemplate.getForObject(shortTokenUrl, TokenResponse.class);
        assert shortRes != null;
        String shortToken = shortRes.accessToken();

        String longTokenUrl = "https://graph.facebook.com/v25.0/oauth/access_token" +
            "?grant_type=fb_exchange_token" +
            "&client_id=" + facebookClientId +
            "&client_secret=" + facebookClientSecret +
            "&fb_exchange_token=" + shortToken;

        TokenResponse longRes = restTemplate.getForObject(longTokenUrl, TokenResponse.class);
        assert longRes != null;
        return longRes;
    }

    public List<Page> fetchFacebookPages(String longToken) {
        String url = "https://graph.facebook.com/v25.0/me/accounts" +
            "?fields=id,name,access_token,picture" +
            "&access_token=" + longToken;

        FacebookResponse res = restTemplate.getForObject(url, FacebookResponse.class);
        return (res != null && res.data() != null) ? res.data() : List.of();
    }
}

