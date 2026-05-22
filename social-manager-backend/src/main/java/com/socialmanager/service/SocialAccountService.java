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

    @Value("${AES_SECRET}")
    private String aesSecret;

    /**
     * Helper method to find user by email or username
     * Supports both Google OAuth (email) and local login (username)
     */
    private User findUserByIdentifier(String identifier) {
        return userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByUsername(identifier))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + identifier));
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
        SocialAccount account = socialAccountRepository.findByUserIdAndPlatformAndExternalAccountId(user.getId(), platform, externalId).orElseGet(() -> SocialAccount.builder().user(user).platform(platform).isAutoPilot(false).build());

        account.setExternalAccountId(externalId);
        account.setAccountName(name);
        account.setAccountAlias(alias);
        account.setProfilePictureUrl(pictureUrl);
        account.setAccessToken(EncryptionUtil.encrypt(accessToken, aesSecret));
        account.setRefreshToken(refreshToken != null ? EncryptionUtil.encrypt(refreshToken, aesSecret) : null);

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
        SocialAccount account = socialAccountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + accountId));

        TokenResponse newTokens = null;

        try {
            switch (account.getPlatform()) {
                case INSTAGRAM -> {
                    String currentAccessToken = EncryptionUtil.decrypt(account.getAccessToken(), aesSecret);
                    newTokens = instagramClient.refreshAccessToken(currentAccessToken);
                }
                case THREADS -> {
                    String currentAccessToken = EncryptionUtil.decrypt(account.getAccessToken(), aesSecret);
                    newTokens = threadsClient.refreshAccessToken(currentAccessToken);
                }
                case TIKTOK -> {
                    String currentRefreshToken = EncryptionUtil.decrypt(account.getRefreshToken(), aesSecret);
                    newTokens = tikTokClient.refreshAccessToken(currentRefreshToken);
                }
                case FACEBOOK -> {
                    return;
                }
            }

            if (newTokens == null) {
                throw new RuntimeException("Không thể làm mới token từ nền tảng");
            }

            account.setAccessToken(EncryptionUtil.encrypt(newTokens.accessToken(), aesSecret));
            if (newTokens.refreshToken() != null) {
                account.setRefreshToken(EncryptionUtil.encrypt(newTokens.refreshToken(), aesSecret));
            }
            if (newTokens.expiresIn() != null) {
                account.setExpiresAt(LocalDateTime.now().plusSeconds(newTokens.expiresIn()));
            }
            socialAccountRepository.save(account);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi giải mã/mã hóa khi refresh token", e);
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

    public void deleteSocialAccountById(UUID id, String username) {
        User user = findUserByIdentifier(username);

        SocialAccount account = socialAccountRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Social account not found"));

        if (!account.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Bạn không có quyền xóa tài khoản mạng xã hội này");
        }


        socialAccountRepository.delete(account);
    }
}