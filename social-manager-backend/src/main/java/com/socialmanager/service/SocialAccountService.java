package com.socialmanager.service;

import com.socialmanager.dto.SocialAccountDto;
import com.socialmanager.model.Platform;
import com.socialmanager.model.SocialAccount;
import com.socialmanager.repository.SocialAccountRepository;
import com.socialmanager.repository.UserRepository;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

record MetaPage(
        String id,
        String name,
        String pageToken,
        String pictureUrl
) {}

record MetaInstagram(
        String id,
        String username,
        String pictureUrl
) {}

@Service
@RequiredArgsConstructor
public class SocialAccountService {

    private final SocialAccountRepository socialAccountRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate(); // Hoặc inject từ Config

    public static final Map<String, String> pkceStorage = new ConcurrentHashMap<>();

    @Value("${AES_SECRET}")
    private String aesSecret;

    @Value("${META_APP_ID}")
    private String metaAppId;

    @Value("${META_APP_SECRET}")
    private String metaAppSecret;

    @Value("${META_REDIRECT_URI}")
    private String metaRedirectUri;

    @Value("${TIKTOK_CLIENT_KEY}")
    private String tiktokClientKey;

    @Value("${TIKTOK_CLIENT_SECRET}")
    private String tiktokClientSecret;

    @Value("${TIKTOK_REDIRECT_URI}")
    private String tiktokRedirectUri;


    public String generateAuthUrl(Platform platform) {
        return switch (platform) {
            case FACEBOOK -> String.format(
                    "https://www.facebook.com/v25.0/dialog/oauth?" +
                            "client_id=%s" +
                            "&redirect_uri=%s" +
                            "&response_type=code" +
                            "&scope=pages_manage_metadata,pages_manage_posts,pages_read_engagement,pages_show_list",
                    metaAppId,
                    metaRedirectUri
            );
            case TIKTOK -> {
                String state = UUID.randomUUID().toString();
                String codeVerifier = PKCEUtil.generateCodeVerifier();
                String codeChallenge = PKCEUtil.generateCodeChallenge(codeVerifier);

                pkceStorage.put(state, codeVerifier);

                // Nối chuỗi URL
                yield String.format(
                        "https://www.tiktok.com/v2/auth/authorize/" +
                                "?client_key=%s" +
                                "&redirect_uri=%s" +
                                "&response_type=code" +
                                "&scope=user.info.basic" +
                                "&state=%s" +
                                "&code_challenge=%s" +
                                "&code_challenge_method=S256", // Bắt buộc phải khai báo S256
                        tiktokClientKey,
                        URLEncoder.encode(tiktokRedirectUri, StandardCharsets.UTF_8),
                        state,
                        codeChallenge
                );
            }

            default -> throw new IllegalArgumentException("Unsupported platform");
        };
    }

    private String exchangeCodeForMetaLongToken(String code) {

        // short token
        String shortTokenUrl = "https://graph.facebook.com/oauth/access_token" +
                "?client_id=" + metaAppId +
                "&client_secret=" + metaAppSecret +
                "&redirect_uri=" + metaRedirectUri +
                "&code=" + code;

        Map shortRes = restTemplate.getForObject(shortTokenUrl, Map.class);
        assert shortRes != null;
        String shortToken = (String) shortRes.get("access_token");

        // long token
        String longTokenUrl = "https://graph.facebook.com/v25.0/oauth/access_token" +
                "?grant_type=fb_exchange_token" +
                "&client_id=" + metaAppId +
                "&client_secret=" + metaAppSecret +
                "&fb_exchange_token=" + shortToken;

        Map longRes = restTemplate.getForObject(longTokenUrl, Map.class);

        assert longRes != null;
        return (String) longRes.get("access_token");
    }

    private List<MetaPage> fetchFacebookPages(String userToken) {

        String url = "https://graph.facebook.com/v25.0/me/accounts" +
                "?fields=id,name,access_token,picture" +
                "&access_token=" + userToken;

        Map res = restTemplate.getForObject(url, Map.class);
        List<Map<String,Object>> data = (List<Map<String,Object>>) res.get("data");

        List<MetaPage> pages = new ArrayList<>();

        for (Map<String,Object> p : data) {

            Map picture = (Map) p.get("picture");
            Map pictureData = (Map) picture.get("data");

            pages.add(new MetaPage(
                    (String) p.get("id"),
                    (String) p.get("name"),
                    (String) p.get("access_token"),
                    (String) pictureData.get("url")
            ));
        }

        return pages;
    }

//    private SocialAccount saveFacebookPage(User user, MetaPage page) throws Exception {
//
//        String encryptedToken = EncryptionUtil.encrypt(page.pageToken(), aesSecret);
//
//        SocialAccount account = socialAccountRepository
//                .findByUserIdAndPlatformAndExternalAccountId(
//                        user.getId(), Platform.FACEBOOK, page.id())
//                .orElse(new SocialAccount());
//
//        account.setUser(user);
//        account.setPlatform(Platform.FACEBOOK);
//        account.setExternalAccountId(page.id());
//        account.setAccountName(page.name());
//        account.setAccountAlias(page.name());
//        account.setProfilePictureUrl(page.pictureUrl());
//        account.setAccessToken(encryptedToken);
//
//        return socialAccountRepository.save(account);
//    }

//    private MetaInstagram fetchInstagramAccount(MetaPage page) {
//
//        String url = "https://graph.facebook.com/v25.0/" + page.id() +
//                "?fields=instagram_business_account" +
//                "&access_token=" + page.pageToken();
//
//        Map res = restTemplate.getForObject(url, Map.class);
//
//        Map ig = (Map) res.get("instagram_business_account");
//        if (ig == null) return null;
//
//        String igId = (String) ig.get("id");
//
//        // lấy info IG
//        String igInfoUrl = "https://graph.facebook.com/v25.0/" + igId +
//                "?fields=username,profile_picture_url" +
//                "&access_token=" + page.pageToken();
//
//        Map igInfo = restTemplate.getForObject(igInfoUrl, Map.class);
//
//        return new MetaInstagram(
//                igId,
//                (String) igInfo.get("username"),
//                (String) igInfo.get("profile_picture_url")
//        );
//    }

//    private SocialAccount saveInstagramAccount(User user, MetaPage page, MetaInstagram ig) throws Exception {
//
//        String encryptedToken = EncryptionUtil.encrypt(page.pageToken(), aesSecret);
//
//        SocialAccount account = socialAccountRepository
//                .findByUserIdAndPlatformAndExternalAccountId(
//                        user.getId(), Platform.INSTAGRAM, ig.id())
//                .orElse(new SocialAccount());
//
//        account.setUser(user);
//        account.setPlatform(Platform.INSTAGRAM);
//        account.setExternalAccountId(ig.id());
//        account.setAccountName(ig.username());
//        account.setAccountAlias(ig.username());
//        account.setProfilePictureUrl(ig.pictureUrl());
//        account.setAccessToken(encryptedToken);
//
//        return socialAccountRepository.save(account);
//    }

    @Transactional
    public void connectMetaAccount(String code, String username) throws Exception {

    //    User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

    // 1. đổi code → long lived token
    String longToken = exchangeCodeForMetaLongToken(code);


    // ----------------------- TEST USER ---------------------
        String url = "https://graph.facebook.com/me"
                + "?fields=id,name"
                + "&access_token=" + longToken;

        Map res = restTemplate.getForObject(url, Map.class);
        assert res != null;
        System.out.println("id: " + res.get("id"));
        System.out.println("name: " + res.get("name"));


    // 2. lấy danh sách pages
    List<MetaPage> pages = fetchFacebookPages(longToken);
    System.out.println("Meta page list: " + pages);

    List<SocialAccountDto> result = new ArrayList<>();

//    for (MetaPage page : pages) {

//         //3. lưu Facebook page
//        SocialAccount fbAccount = saveFacebookPage(user, page);
//        result.add(mapToDto(fbAccount));

        // 4. check instagram business
//        MetaInstagram ig = fetchInstagramAccount(page);
//        System.out.println(ig);

//        if (ig != null) {
//            SocialAccount igAccount = saveInstagramAccount(user, page, ig);
//            result.add(mapToDto(igAccount));
//        }
//    }

    }


    // Thêm tham số codeVerifier vào hàm
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

    @Transactional
    public void connectTikTokAccount(String code, String codeVerifier, String username) throws Exception {
        // Gọi hàm đổi token đã sửa ở trên (nhớ truyền codeVerifier)
        String accessToken = exchangeCodeForTikTokAccessToken(code, codeVerifier);

        // Endpoint V2. Bắt buộc phải khai báo biến `fields` để lấy data (avatar_url thay cho profile_image_url)
        String userInfoUrl = "https://open.tiktokapis.com/v2/user/info/?fields=open_id,union_id,avatar_url,display_name";

        // V2 bắt buộc truyền Token qua Header (Bearer auth), KHÔNG truyền qua query param
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        // Dùng restTemplate.exchange để có thể đính kèm Header (getForObject không làm được)
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

        // Response V2 bọc data trong "data.user"
        Map<String, Object> data = (Map<String, Object>) userInfoRes.get("data");
        Map<String, Object> userNode = (Map<String, Object>) data.get("user");

        String displayName = (String) userNode.get("display_name");
        String profileImage = (String) userNode.get("avatar_url"); // Ở v2 gọi là avatar_url

        System.out.println("TikTok Display name: " + displayName);
        System.out.println("TikTok Profile Image: " + profileImage);

        // TODO: Lưu thông tin vào Database cho user tương ứng...
    }

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
}