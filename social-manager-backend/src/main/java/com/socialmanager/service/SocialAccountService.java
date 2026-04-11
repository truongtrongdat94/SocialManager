package com.socialmanager.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.socialmanager.dto.SocialAccountDto;
import com.socialmanager.model.Platform;
import com.socialmanager.model.SocialAccount;
import com.socialmanager.model.User;
import com.socialmanager.repository.SocialAccountRepository;
import com.socialmanager.repository.UserRepository;
import com.socialmanager.util.EncryptionUtil;
import com.socialmanager.util.JwtUtil;
import com.socialmanager.util.PKCEUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SocialAccountService {

    private final SocialAccountRepository socialAccountRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final JwtUtil jwtUtil;
    public static final Map<String, String> pkceStorage = new ConcurrentHashMap<>();

    private record Picture(
        PictureData data
    ) {
    }

    public record PictureData(
        String url
    ) {
    }

    private record FacebookPage(
        @JsonProperty("id")
        String id,

        @JsonProperty("name")
        String name,

        @JsonProperty("access_token")
        String pageToken,

        @JsonProperty
        Picture picture
    ) {
        public String pictureUrl() {
            if (picture == null || picture.data() == null) {
                return null;
            }
            return picture.data().url();
        }
    }

    private record FacebookResponse(
        List<FacebookPage> data
    ) {
    }

    private record Instagram(
        @JsonProperty("id")
        String id,

        @JsonProperty("username")
        String username,

        @JsonProperty("name")
        String name,

        String accessToken,

        @JsonProperty("profile_picture_url")
        String pictureUrl
    ) {
    }

    private record Threads(
        @JsonProperty("id")
        String id,

        @JsonProperty("username")
        String username,

        @JsonProperty("name")
        String name,

        String accessToken,

        @JsonProperty("threads_profile_picture_url")
        String pictureUrl
    ) {
    }

    private record TikTok(
        @JsonProperty("open_id")
        String id,

        @JsonProperty("display_name")
        String name,

        String accessToken,

        @JsonProperty("avatar_url")
        String pictureUrl
    ) {
    }

    private record TikTokResponse(
       TikTokData data
    ) {}

    private record TikTokData(
        TikTok user
    ) {}

    private record TokenResponse(
        @JsonProperty("access_token")
        String accessToken
    ) {
    }

    @Value("${AES_SECRET}")
    private String aesSecret;

    @Value("${FACEBOOK_CLIENT_ID}")
    private String facebookClientId;

    @Value("${FACEBOOK_CLIENT_SECRET}")
    private String facebookClientSecret;

    @Value("${FACEBOOK_REDIRECT_URI}")
    private String facebookRedirectUri;

    @Value("${INSTAGRAM_CLIENT_ID}")
    private String instagramClientId;

    @Value("${INSTAGRAM_CLIENT_SECRET}")
    private String instagramClientSecret;

    @Value("${INSTAGRAM_REDIRECT_URI}")
    private String instagramRedirectUri;

    @Value("${THREADS_CLIENT_ID}")
    private String threadsClientId;

    @Value("${THREADS_CLIENT_SECRET}")
    private String threadsClientSecret;

    @Value("${THREADS_REDIRECT_URI}")
    private String threadsRedirectUri;

    @Value("${TIKTOK_CLIENT_KEY}")
    private String tiktokClientKey;

    @Value("${TIKTOK_CLIENT_SECRET}")
    private String tiktokClientSecret;

    @Value("${TIKTOK_REDIRECT_URI}")
    private String tiktokRedirectUri;

    private SocialAccountDto mapToDto(SocialAccount account) {
        return new SocialAccountDto(
            account.getId(),
            account.getPlatform(),
            account.getAccountName(),
            account.getAccountAlias(),
            account.getProfilePictureUrl(),
            account.getIsAutoPilot()
        );
    }

    public String generateAuthUrl(Platform platform, String username) {
        String stateJwt = jwtUtil.generateToken(username);

        return switch (platform) {
            case FACEBOOK -> String.format(
                "https://www.facebook.com/v25.0/dialog/oauth" +
                    "?client_id=%s" +
                    "&redirect_uri=%s" +
                    "&response_type=code" +
                    "&scope=pages_manage_metadata,pages_manage_posts,pages_read_engagement,pages_show_list" +
                    "&state=%s",
                facebookClientId,
                URLEncoder.encode(facebookRedirectUri, StandardCharsets.UTF_8),
                stateJwt
            );

            case INSTAGRAM -> String.format(
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

            case THREADS -> String.format(
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

            case TIKTOK -> {
                String codeVerifier = PKCEUtil.generateCodeVerifier();
                String codeChallenge = PKCEUtil.generateCodeChallenge(codeVerifier);

                pkceStorage.put(stateJwt, codeVerifier);

                yield String.format(
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
        };
    }

    private void saveSocialAccountToDatabase(User user, Platform platform, String externalId, String name, String alias, String pictureUrl, String token) throws Exception {
        SocialAccount account = socialAccountRepository
            .findByUserIdAndPlatformAndExternalAccountId(user.getId(), platform, externalId)
            .orElseGet(() -> SocialAccount.builder()
                .user(user)
                .platform(platform)
                .isAutoPilot(false)
                .build());

        account.setExternalAccountId(externalId);
        account.setAccountName(name);
        account.setAccountAlias(alias);
        account.setProfilePictureUrl(pictureUrl);
        account.setAccessToken(EncryptionUtil.encrypt(token, aesSecret));

        socialAccountRepository.save(account);
    }


    // FACEBOOK
    private String exchangeCodeForFacebookLongToken(String code) {
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
        return longRes.accessToken();
    }

    private List<FacebookPage> fetchFacebookPages(String userToken) {
        String url = "https://graph.facebook.com/v25.0/me/accounts" +
            "?fields=id,name,access_token,picture" +
            "&access_token=" + userToken;

        FacebookResponse res = restTemplate.getForObject(url, FacebookResponse.class);
        return res != null ? res.data() : List.of();
    }

    @Transactional
    public void connectFacebookAccount(String code, String username) throws Exception {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

        String longToken = exchangeCodeForFacebookLongToken(code);

        List<FacebookPage> pages = fetchFacebookPages(longToken);
        System.out.println("Page list: " + pages);

        for (FacebookPage page : pages) {
            saveSocialAccountToDatabase(user, Platform.FACEBOOK, page.id(), page.name(), page.name(), page.pictureUrl(), page.pageToken());
        }
    }


    // INSTAGRAM
    private String exchangeCodeForInstagramLongToken(String code) {
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

        return longTokenRes.accessToken();
    }

    private Instagram fetchInstagramAccount(String token) {
        String userInfoUrl = "https://graph.instagram.com/v25.0/me"
            + "?fields=id,username,name,profile_picture_url"
            + "&access_token=" + token;

        Instagram response = restTemplate.getForObject(userInfoUrl, Instagram.class);
        if (response == null || response.id() == null) {
            throw new RuntimeException("Failed to fetch Instagram user info");
        }

        return new Instagram(response.id(), response.username(), response.name(), token, response.pictureUrl());
    }

    @Transactional
    public void connectInstagramAccount(String code, String username) throws Exception {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        String longToken = exchangeCodeForInstagramLongToken(code);
        Instagram account = fetchInstagramAccount(longToken);
        System.out.println("Instagram account: " + account);
        saveSocialAccountToDatabase(user, Platform.INSTAGRAM, account.id(), account.username(), account.name(), account.pictureUrl(), account.accessToken());
    }


    // THREADS
    private String exchangeCodeForThreadsLongToken(String code) {
        if (code.endsWith("#_")) {
            code = code.substring(0, code.length() - 2);
        }

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

        if (shortTokenRes == null || shortTokenRes.accessToken() == null) {
            throw new RuntimeException("Failed to get Threads short-lived token");
        }
        String shortLivedToken = shortTokenRes.accessToken();

        String longTokenUrl = "https://graph.threads.net/access_token"
            + "?grant_type=th_exchange_token"
            + "&client_secret=" + threadsClientSecret
            + "&access_token=" + shortLivedToken;

        TokenResponse longTokenRes = restTemplate.getForObject(longTokenUrl, TokenResponse.class);

        if (longTokenRes == null || longTokenRes.accessToken() == null) {
            throw new RuntimeException("Failed to get Threads long-lived token");
        }

        return longTokenRes.accessToken();
    }

    private Threads fetchThreadsAccount(String token) {
        String userInfoUrl = "https://graph.threads.net/v1.0/me"
            + "?fields=id,username,name,threads_profile_picture_url"
            + "&access_token=" + token;

        Threads response = restTemplate.getForObject(userInfoUrl, Threads.class);

        if (response == null || response.id() == null) {
            throw new RuntimeException("Failed to fetch Threads user info");
        }

        return new Threads(response.id(), response.username(), response.name(), token, response.pictureUrl());

    }

    @Transactional
    public void connectThreadsAccount(String code, String username) throws Exception {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        String longToken = exchangeCodeForThreadsLongToken(code);
        Threads account = fetchThreadsAccount(longToken);
        System.out.println("Threads account: " + account);
        saveSocialAccountToDatabase(user, Platform.THREADS, account.id(), account.username(), account.name(), account.pictureUrl(), account.accessToken());
    }


    // TIKTOK
    private String exchangeCodeForTikTokAccessToken(String code, String codeVerifier) {
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

        return response.accessToken();
    }

    private TikTok fetchTikTokAccount(String token) {
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

        TikTok user = body.data().user();
        return new TikTok(user.id(), user.name(), token, user.pictureUrl());
    }

    @Transactional
    public void connectTikTokAccount(String code, String codeVerifier, String username) throws Exception {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        String accessToken = exchangeCodeForTikTokAccessToken(code, codeVerifier);
        TikTok account = fetchTikTokAccount(accessToken);
        System.out.println("TikTok account: " + account);
        saveSocialAccountToDatabase(user, Platform.TIKTOK, account.id(), account.name(), account.name(), account.pictureUrl(), account.accessToken());
    }
}