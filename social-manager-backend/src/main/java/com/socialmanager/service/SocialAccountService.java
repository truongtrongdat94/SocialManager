package com.socialmanager.service;

import com.socialmanager.client.*;
import com.socialmanager.dto.*;
import com.socialmanager.dto.external.*;
import com.socialmanager.dto.external.FacebookResponse.Page;
import com.socialmanager.dto.external.TikTokResponse.TikTok;
import com.socialmanager.exception.ResourceNotFoundException;
import com.socialmanager.model.*;
import com.socialmanager.repository.*;
import com.socialmanager.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;


@Service
@RequiredArgsConstructor
public class SocialAccountService {
    private final SocialAccountRepository socialAccountRepository;
    private final UserRepository userRepository;

    private final FacebookClient facebookClient;
    private final InstagramClient instagramClient;
    private final ThreadsClient threadsClient;
    private final TikTokClient tikTokClient;

    private final JwtUtil jwtUtil;
    private final ScheduledPostRepository scheduledPostRepository;
    private final PostHistoryRepository postHistoryRepository;
    private final AutoPilotConfigRepository autoPilotConfigRepository;
    private final AccountDailyInsightRepository accountDailyInsightRepository;

    private final com.socialmanager.config.AesSecretProvider aesSecretProvider;

    /**
     * Helper method to find user by email or username
     * Supports both Google OAuth (email) and local login (username)
     */
    private User findUserByIdentifier(String identifier) {
        return userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByUsername(identifier))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + identifier));
    }

    private void assertAesSecretConfigured() {
        String s = aesSecretProvider.getSecret();
        if (s == null || s.trim().isEmpty()) {
            throw new IllegalStateException("Thiếu cấu hình app.aes-secret (AES_SECRET)");
        }
    }

    private String normalizeAndValidateMediaUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("URL media không hợp lệ");
        }

        String candidate = rawUrl.trim();
        URI uri;
        try {
            uri = URI.create(candidate);
        } catch (Exception ex) {
            throw new IllegalArgumentException("URL media không hợp lệ: " + rawUrl);
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw new IllegalArgumentException("URL media phải bắt đầu bằng http/https");
        }

        // Wikimedia page links like /wiki/File:... are not direct image URLs for Facebook.
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        String path = uri.getPath() == null ? "" : uri.getPath();
        if (host.contains("commons.wikimedia.org") && path.startsWith("/wiki/File:")) {
            String fileName = path.substring("/wiki/File:".length());
            fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8);
            candidate = "https://commons.wikimedia.org/wiki/Special:FilePath/" + URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            uri = URI.create(candidate);
        }

        // Remove fragment because Facebook fetches server-side and fragments are client-side only.
        if (uri.getFragment() != null) {
            try {
                uri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(), null);
            } catch (Exception ex) {
                throw new IllegalArgumentException("URL media không hợp lệ: " + rawUrl);
            }
        }

        String normalized = uri.toString();
        String lower = normalized.toLowerCase(Locale.ROOT);
        boolean hasImageExtension = lower.contains(".jpg")
            || lower.contains(".jpeg")
            || lower.contains(".png")
            || lower.contains(".gif")
            || lower.contains(".webp")
            || lower.contains(".bmp")
            || lower.contains(".svg");

        if (!hasImageExtension) {
            throw new IllegalArgumentException("URL media phải là link ảnh trực tiếp (jpg, jpeg, png, gif, webp, bmp, svg)");
        }

        return normalized;
    }

    private SocialAccountDto mapToDto(SocialAccount account) {
        return new SocialAccountDto(
            account.getId(), 
            account.getPlatform(), 
            account.getExternalAccountId(),
            account.getAccountName(), 
            account.getAccountAlias(), 
            account.getProfilePictureUrl(), 
            account.getIsAutoPilot()
        );
    }

    public UUID getUserIdByUsername(String username) {
        return userRepository.findByUsername(username)
            .map(User::getId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public String generateAuthUrl(Platform platform, String username) {
        String stateJwt = jwtUtil.generateToken(username);
        return switch (platform) {
            case FACEBOOK -> facebookClient.getAuthUrl(stateJwt);
            case INSTAGRAM -> instagramClient.getAuthUrl(stateJwt);
            case THREADS -> threadsClient.getAuthUrl(stateJwt);
            case TIKTOK -> tikTokClient.getAuthUrl(stateJwt);
        };
    }

    private void saveSocialAccountToDatabase(User user, Platform platform, String externalId, String name, String alias, String pictureUrl, String accessToken, String refreshToken, Integer expiresInSeconds) throws Exception {
        assertAesSecretConfigured();
        SocialAccount account = socialAccountRepository.findByUserIdAndPlatformAndExternalAccountId(user.getId(), platform, externalId).orElseGet(() -> SocialAccount.builder().user(user).platform(platform).isAutoPilot(false).build());

        account.setExternalAccountId(externalId);
        account.setAccountName(name);
        account.setAccountAlias(alias);
        account.setProfilePictureUrl(pictureUrl);
        String secret = aesSecretProvider.getSecret();
        account.setAccessToken(EncryptionUtil.encrypt(accessToken, secret));
        account.setRefreshToken(refreshToken != null ? EncryptionUtil.encrypt(refreshToken, secret) : null);

        if (expiresInSeconds != null) {
            account.setExpiresAt(LocalDateTime.now().plusSeconds(expiresInSeconds));
        } else {
            account.setExpiresAt(null);
        }

        socialAccountRepository.save(account);
    }

    @Transactional
    public void connectFacebookAccount(String code, String username) {
        User user = findUserByIdentifier(username);

        TokenResponse tokenResponse = facebookClient.exchangeCodeForFacebookLongToken(code);

        List<Page> pages = facebookClient.fetchFacebookPages(tokenResponse.accessToken());

        for (Page page : pages) {
            try {
                saveSocialAccountToDatabase(user, Platform.FACEBOOK, page.id(), page.name(), page.name(), page.pictureUrl(), page.pageToken(), null, null);
            } catch (Exception e) {
                throw new RuntimeException("Lỗi khi mã hóa và lưu DB", e);
            }
        }
    }

    @Transactional
    public void connectInstagramAccount(String code, String username) {
        User user = findUserByIdentifier(username);
        TokenResponse tokenResponse = instagramClient.exchangeCodeForInstagramLongToken(code);
        InstagramResponse account = instagramClient.fetchInstagramAccount(tokenResponse.accessToken());
        try {
            saveSocialAccountToDatabase(user, Platform.INSTAGRAM, account.id(), account.username(), account.name(), account.pictureUrl(), tokenResponse.accessToken(), null, tokenResponse.expiresIn());
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi mã hóa và lưu DB", e);
        }
    }

    @Transactional
    public void connectThreadsAccount(String code, String username) {
        User user = findUserByIdentifier(username);
        TokenResponse tokenResponse = threadsClient.exchangeCodeForThreadsLongToken(code);
        ThreadsResponse account = threadsClient.fetchThreadsAccount(tokenResponse.accessToken());
        try {
            saveSocialAccountToDatabase(user, Platform.THREADS, account.id(), account.username(), account.name(), account.pictureUrl(), tokenResponse.accessToken(), null, tokenResponse.expiresIn());
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi mã hóa và lưu DB", e);
        }
    }

    @Transactional
    public void connectTikTokAccount(String code, String codeVerifier, String username) {
        User user = findUserByIdentifier(username);
        TokenResponse tokenResponse = tikTokClient.exchangeCodeForTikTokAccessToken(code, codeVerifier);
        TikTok account = tikTokClient.fetchTikTokAccount(tokenResponse.accessToken());
        try {
            saveSocialAccountToDatabase(user, Platform.TIKTOK, account.id(), account.name(), account.name(), account.pictureUrl(), tokenResponse.accessToken(), tokenResponse.refreshToken(), tokenResponse.expiresIn());
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi mã hóa và lưu DB", e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void refreshAccessToken(UUID accountId) {
        assertAesSecretConfigured();
        SocialAccount account = socialAccountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + accountId));

        TokenResponse newTokens = null;

        try {
            switch (account.getPlatform()) {
                case INSTAGRAM -> {
                    String currentAccessToken = EncryptionUtil.decrypt(account.getAccessToken(), aesSecretProvider.getSecret());
                    newTokens = instagramClient.refreshAccessToken(currentAccessToken);
                }
                case THREADS -> {
                    String currentAccessToken = EncryptionUtil.decrypt(account.getAccessToken(), aesSecretProvider.getSecret());
                    newTokens = threadsClient.refreshAccessToken(currentAccessToken);
                }
                case TIKTOK -> {
                    String currentRefreshToken = EncryptionUtil.decrypt(account.getRefreshToken(), aesSecretProvider.getSecret());
                    newTokens = tikTokClient.refreshAccessToken(currentRefreshToken);
                }
                case FACEBOOK -> {
                    return;
                }
            }

            if (newTokens == null) {
                throw new RuntimeException("Không thể làm mới token từ nền tảng");
            }

            account.setAccessToken(EncryptionUtil.encrypt(newTokens.accessToken(), aesSecretProvider.getSecret()));
            if (newTokens.refreshToken() != null) {
                account.setRefreshToken(EncryptionUtil.encrypt(newTokens.refreshToken(), aesSecretProvider.getSecret()));
            }
            if (newTokens.expiresIn() != null) {
                account.setExpiresAt(LocalDateTime.now().plusSeconds(newTokens.expiresIn()));
            }
            socialAccountRepository.save(account);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi giải mã/mã hóa khi refresh token", e);
        }


    }

    @Transactional
    public String publishFacebookPost(UUID accountId, String username, String caption, List<String> mediaUrls) {
        return publishFacebookPost(accountId, username, caption, mediaUrls, true);
    }

    @Transactional
    public String publishFacebookPost(UUID accountId, String username, String caption, List<String> mediaUrls, boolean saveHistory) {
        assertAesSecretConfigured();
        UUID userId = userRepository.findByUsername(username).map(User::getId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SocialAccount account = socialAccountRepository.findByIdAndUserId(accountId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Social account not found or access denied"));

        if (account.getPlatform() != Platform.FACEBOOK) {
            throw new IllegalArgumentException("Social account is not a Facebook account");
        }

        try {
            String pageAccessToken = EncryptionUtil.decrypt(account.getAccessToken(), aesSecretProvider.getSecret());
            String pageId = account.getExternalAccountId();

            String publishedId;
            if (mediaUrls == null || mediaUrls.isEmpty()) {
                publishedId = facebookClient.publishTextPost(pageId, pageAccessToken, caption).id();
            } else {
                List<String> photoIds = new ArrayList<>();
                for (String mediaUrl : mediaUrls) {
                    String normalizedUrl = normalizeAndValidateMediaUrl(mediaUrl);
                    photoIds.add(facebookClient.uploadPhoto(pageId, pageAccessToken, normalizedUrl, false).id());
                }

                publishedId = facebookClient.publishPhotoFeedPost(pageId, pageAccessToken, caption, photoIds).id();
            }

            if (saveHistory) {
                try {
                    ScheduledPost sp = ScheduledPost.builder()
                        .user(account.getUser())
                        .socialAccount(account)
                        .caption(caption)
                        .mediaUrls(mediaUrls != null ? mediaUrls.toArray(new String[0]) : null)
                        .scheduledTime(LocalDateTime.now())
                        .status("POSTED")
                        .publishedPostId(publishedId)
                        .build();

                    scheduledPostRepository.save(sp);
                } catch (Exception e) {
                    System.err.println("Failed to save publish history: " + e.getMessage());
                }
            }

            return publishedId;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi đăng bài Facebook", e);
        }
    }

    public SocialAccountDto getSocialAccountByIdAndUsername(UUID id, String username) {
        User user = findUserByIdentifier(username);
        UUID userId = user.getId();

        SocialAccount account = socialAccountRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Social account not found or access denied"));

        return mapToDto(account);
    }

    public List<SocialAccountDto> getSocialAccountsByUsername(String username) {
        User user = findUserByIdentifier(username);
        UUID userId = user.getId();

        List<SocialAccount> accounts = socialAccountRepository.findByUserId(userId);

        return accounts.stream().map(this::mapToDto).toList();
    }

    @Transactional
    public void deleteSocialAccountById(UUID id, String username) {
        User user = findUserByIdentifier(username);

        SocialAccount account = socialAccountRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Social account not found"));

        if (!account.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Bạn không có quyền xóa tài khoản mạng xã hội này");
        }

        scheduledPostRepository.deleteBySocialAccount_Id(account.getId());
        autoPilotConfigRepository.deleteBySocialAccount_Id(account.getId());
        accountDailyInsightRepository.deleteBySocialAccount_Id(account.getId());

        socialAccountRepository.delete(account);
    }
}