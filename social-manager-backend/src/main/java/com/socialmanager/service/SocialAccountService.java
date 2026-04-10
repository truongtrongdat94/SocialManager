package com.socialmanager.service;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SocialAccountService {

    private final SocialAccountRepository socialAccountRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final JwtUtil jwtUtil;
    public static final Map<String, String> pkceStorage = new ConcurrentHashMap<>();

    private record MetaFacebookPage(
        String id,
        String name,
        String pageToken,
        String pictureUrl
    ) {
    }

    private record MetaInstagram(
        String id,
        String username,
        String name,
        String accessToken,
        String pictureUrl
    ) {
    }

    private record MetaThreads(
        String id,
        String username,
        String name,
        String accessToken,
        String pictureUrl
    ) {
    }

    private record TikTok(
        String id,
        String name,
        String accessToken,
        String pictureUrl
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

        // short token
        String shortTokenUrl = "https://graph.facebook.com/oauth/access_token" +
            "?client_id=" + facebookClientId +
            "&client_secret=" + facebookClientSecret +
            "&redirect_uri=" + facebookRedirectUri +
            "&code=" + code;

        Map shortRes = restTemplate.getForObject(shortTokenUrl, Map.class);
        assert shortRes != null;
        String shortToken = (String) shortRes.get("access_token");

        // long token
        String longTokenUrl = "https://graph.facebook.com/v25.0/oauth/access_token" +
            "?grant_type=fb_exchange_token" +
            "&client_id=" + facebookClientId +
            "&client_secret=" + facebookClientSecret +
            "&fb_exchange_token=" + shortToken;

        Map longRes = restTemplate.getForObject(longTokenUrl, Map.class);

        assert longRes != null;
        return (String) longRes.get("access_token");
    }

    private List<MetaFacebookPage> fetchFacebookPages(String userToken) {
        String url = "https://graph.facebook.com/v25.0/me/accounts" +
            "?fields=id,name,access_token,picture" +
            "&access_token=" + userToken;

        Map res = restTemplate.getForObject(url, Map.class);
        List<Map<String, Object>> data = (List<Map<String, Object>>) res.get("data");

        List<MetaFacebookPage> pages = new ArrayList<>();

        for (Map<String, Object> p : data) {

            Map picture = (Map) p.get("picture");
            Map pictureData = (Map) picture.get("data");

            pages.add(new MetaFacebookPage(
                (String) p.get("id"),
                (String) p.get("name"),
                (String) p.get("access_token"),
                (String) pictureData.get("url")
            ));
        }

        return pages;
    }

    @Transactional
    public void connectFacebookAccount(String code, String username) throws Exception {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

        String longToken = exchangeCodeForFacebookLongToken(code);

        List<MetaFacebookPage> pages = fetchFacebookPages(longToken);
        System.out.println("Page list: " + pages);

        for (MetaFacebookPage page : pages) {
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
        body.add("client_id", instagramClientId);         // App ID của Instagram
        body.add("client_secret", instagramClientSecret); // App Secret của Instagram
        body.add("grant_type", "authorization_code");
        body.add("redirect_uri", instagramRedirectUri);
        body.add("code", code);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        String shortLivedToken;

        try {
            Map shortTokenRes = restTemplate.postForObject(shortTokenUrl, request, Map.class);

            if (shortTokenRes == null || !shortTokenRes.containsKey("access_token")) {
                System.err.println("Instagram Short Token Error: " + shortTokenRes);
                throw new RuntimeException("Failed to get Instagram short-lived token");
            }
            shortLivedToken = (String) shortTokenRes.get("access_token");

        } catch (Exception e) {
            System.err.println("Lỗi gọi API Instagram (1): " + e.getMessage());
            throw e;
        }

        String longTokenUrl = "https://graph.instagram.com/access_token"
            + "?grant_type=ig_exchange_token"
            + "&client_secret=" + instagramClientSecret
            + "&access_token=" + shortLivedToken;

        try {
            Map longTokenRes = restTemplate.getForObject(longTokenUrl, Map.class);

            if (longTokenRes == null || !longTokenRes.containsKey("access_token")) {
                System.err.println("Instagram Long Token Error: " + longTokenRes);
                throw new RuntimeException("Failed to get Instagram long-lived token");
            }

            return (String) longTokenRes.get("access_token");

        } catch (Exception e) {
            System.err.println("Lỗi gọi API Instagram (2): " + e.getMessage());
            throw e;
        }
    }

    private MetaInstagram fetchInstagramAccount(String token) {
        String userInfoUrl = "https://graph.instagram.com/v25.0/me"
            + "?fields=id,username,name,profile_picture_url"
            + "&access_token=" + token;

        try {
            Map response = restTemplate.getForObject(userInfoUrl, Map.class);

            if (response == null || !response.containsKey("id")) {
                System.err.println("Instagram User Info Error: " + response);
                throw new RuntimeException("Failed to fetch Instagram user info");
            }

            String id = (String) response.get("id");
            String username = (String) response.get("username");
            String name = response.containsKey("name") ? (String) response.get("name") : username;
            String pictureUrl = (String) response.get("profile_picture_url");

            return new MetaInstagram(id, username, name, token, pictureUrl);

        } catch (Exception e) {
            System.err.println("Lỗi gọi API lấy thông tin Instagram: " + e.getMessage());
            throw e;
        }
    }

    @Transactional
    public void connectInstagramAccount(String code, String username) throws Exception {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        String longToken = exchangeCodeForInstagramLongToken(code);
        MetaInstagram account = fetchInstagramAccount(longToken);
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
        String shortLivedToken;

        try {
            Map shortTokenRes = restTemplate.postForObject(shortTokenUrl, request, Map.class);

            if (shortTokenRes == null || !shortTokenRes.containsKey("access_token")) {
                System.err.println("Threads Short Token Error: " + shortTokenRes);
                throw new RuntimeException("Failed to get Threads short-lived token");
            }
            shortLivedToken = (String) shortTokenRes.get("access_token");

        } catch (Exception e) {
            System.err.println("Lỗi gọi API Threads (1): " + e.getMessage());
            throw e;
        }

        String longTokenUrl = "https://graph.threads.net/access_token"
            + "?grant_type=th_exchange_token"
            + "&client_secret=" + threadsClientSecret
            + "&access_token=" + shortLivedToken;

        try {
            Map longTokenRes = restTemplate.getForObject(longTokenUrl, Map.class);

            if (longTokenRes == null || !longTokenRes.containsKey("access_token")) {
                System.err.println("Threads Long Token Error: " + longTokenRes);
                throw new RuntimeException("Failed to get Threads long-lived token");
            }

            return (String) longTokenRes.get("access_token");

        } catch (Exception e) {
            System.err.println("Lỗi gọi API Threads (2): " + e.getMessage());
            throw e;
        }
    }

    private MetaThreads fetchThreadsAccount(String token) {
        String userInfoUrl = "https://graph.threads.net/v1.0/me"
            + "?fields=id,username,name,threads_profile_picture_url"
            + "&access_token=" + token;

        try {
            Map response = restTemplate.getForObject(userInfoUrl, Map.class);

            if (response == null || !response.containsKey("id")) {
                System.err.println("Threads User Info Error: " + response);
                throw new RuntimeException("Failed to fetch Threads user info");
            }

            String id = (String) response.get("id");
            String username = (String) response.get("username");
            String name = response.containsKey("name") ? (String) response.get("name") : username;
            String pictureUrl = (String) response.get("threads_profile_picture_url");

            return new MetaThreads(id, username, name, token, pictureUrl);

        } catch (Exception e) {
            System.err.println("Lỗi gọi API lấy thông tin Threads: " + e.getMessage());
            throw e;
        }
    }

    @Transactional
    public void connectThreadsAccount(String code, String username) throws Exception {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        String longToken = exchangeCodeForThreadsLongToken(code);
        MetaThreads account = fetchThreadsAccount(longToken);
        System.out.println("Threads account: " + account);
        saveSocialAccountToDatabase(user, Platform.THREADS, account.id(), account.username(), account.name(), account.pictureUrl(), account.accessToken());
    }


    // TIKTOK
    private String exchangeCodeForTikTokAccessToken(String code, String codeVerifier) {
        // Endpoint V2 của TikTok
        String tokenUrl = "https://open.tiktokapis.com/v2/oauth/token/";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_key", tiktokClientKey);
        body.add("client_secret", tiktokClientSecret);
        body.add("code", code);
        body.add("grant_type", "authorization_code");
        body.add("redirect_uri", tiktokRedirectUri); // V2 bắt buộc truyền redirect_uri
        body.add("code_verifier", codeVerifier);     // V2 bắt buộc truyền code_verifier để verify PKCE

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        // V2 của TikTok yêu cầu Cache-Control: no-cache cho api lấy token
        headers.setCacheControl(CacheControl.noCache());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        Map response = restTemplate.postForObject(tokenUrl, request, Map.class);

        // API V2 trả thẳng access_token ra ngoài, không bọc trong "data" nữa
        if (response == null || !response.containsKey("access_token")) {
            System.err.println("TikTok Token Error: " + response);
            throw new RuntimeException("Failed to get TikTok access token");
        }

        return (String) response.get("access_token");
    }

    private TikTok fetchTikTokAccount(String token) {
        String userInfoUrl = "https://open.tiktokapis.com/v2/user/info/?fields=open_id,union_id,avatar_url,display_name";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map> responseEntity = restTemplate.exchange(
            userInfoUrl,
            HttpMethod.GET,
            request,
            Map.class
        );

        Map userInfoRes = responseEntity.getBody();

        if (userInfoRes == null || !userInfoRes.containsKey("data")) {
            System.err.println("TikTok User Info Error: " + userInfoRes);
            throw new RuntimeException("Failed to fetch TikTok user info");
        }

        Map<String, Object> data = (Map<String, Object>) userInfoRes.get("data");
        Map<String, Object> userNode = (Map<String, Object>) data.get("user");

        String openId = (String) userNode.get("open_id");
        String displayName = (String) userNode.get("display_name");
        String profileImage = (String) userNode.get("avatar_url");

        System.out.println("TikTok Display name: " + displayName);
        System.out.println("TikTok Profile Image: " + profileImage);

        return new TikTok(openId, displayName, token, profileImage);
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