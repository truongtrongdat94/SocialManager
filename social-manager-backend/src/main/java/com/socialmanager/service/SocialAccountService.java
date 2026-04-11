package com.socialmanager.service;

import com.socialmanager.client.*;
import com.socialmanager.dto.*;
import com.socialmanager.dto.external.*;
import com.socialmanager.dto.external.FacebookResponse.Page;
import com.socialmanager.dto.external.TikTokResponse.TikTok;
import com.socialmanager.model.*;
import com.socialmanager.repository.*;
import com.socialmanager.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
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

    private SocialAccountDto mapToDto(SocialAccount account) {
        return new SocialAccountDto(account.getId(), account.getPlatform(), account.getAccountName(), account.getAccountAlias(), account.getProfilePictureUrl(), account.getIsAutoPilot());
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
    public void connectFacebookAccount(String code, String username) throws Exception {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

        TokenResponse tokenResponse = facebookClient.exchangeCodeForFacebookLongToken(code);

        List<Page> pages = facebookClient.fetchFacebookPages(tokenResponse.accessToken());
        System.out.println("Page list: " + pages);

        for (Page page : pages) {
            saveSocialAccountToDatabase(user, Platform.FACEBOOK, page.id(), page.name(), page.name(), page.pictureUrl(), page.pageToken(), null, null);
        }
    }

    @Transactional
    public void connectInstagramAccount(String code, String username) throws Exception {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        TokenResponse tokenResponse = instagramClient.exchangeCodeForInstagramLongToken(code);
        InstagramResponse account = instagramClient.fetchInstagramAccount(tokenResponse.accessToken());
        System.out.println("Instagram account: " + account);
        saveSocialAccountToDatabase(user, Platform.INSTAGRAM, account.id(), account.username(), account.name(), account.pictureUrl(), tokenResponse.accessToken(), null, tokenResponse.expiresIn());
    }

    @Transactional
    public void connectThreadsAccount(String code, String username) throws Exception {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        TokenResponse tokenResponse = threadsClient.exchangeCodeForThreadsLongToken(code);
        ThreadsResponse account = threadsClient.fetchThreadsAccount(tokenResponse.accessToken());
        System.out.println("Threads account: " + account);
        saveSocialAccountToDatabase(user, Platform.THREADS, account.id(), account.username(), account.name(), account.pictureUrl(), tokenResponse.accessToken(), null, tokenResponse.expiresIn());
    }

    @Transactional
    public void connectTikTokAccount(String code, String codeVerifier, String username) throws Exception {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        TokenResponse tokenResponse = tikTokClient.exchangeCodeForTikTokAccessToken(code, codeVerifier);
        TikTok account = tikTokClient.fetchTikTokAccount(tokenResponse.accessToken());
        System.out.println("TikTok account: " + account);
        saveSocialAccountToDatabase(user, Platform.TIKTOK, account.id(), account.name(), account.name(), account.pictureUrl(), tokenResponse.accessToken(), tokenResponse.refreshToken(), tokenResponse.expiresIn());
    }

    @Transactional(rollbackFor = Exception.class)
    public void refreshAccessToken(UUID accountId) throws Exception {
        SocialAccount account = socialAccountRepository.findById(accountId).orElseThrow(() -> new RuntimeException("Account not found with ID: " + accountId));

        TokenResponse newTokens = null;

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
        }

        assert newTokens != null;
        account.setAccessToken(EncryptionUtil.encrypt(newTokens.accessToken(), aesSecret));
        if (newTokens.refreshToken() != null) {
            account.setRefreshToken(EncryptionUtil.encrypt(newTokens.refreshToken(), aesSecret));
        }
        if (newTokens.expiresIn() != null) {
            account.setExpiresAt(LocalDateTime.now().plusSeconds(newTokens.expiresIn()));
        }
        socialAccountRepository.save(account);
    }

    public List<SocialAccountDto> getSocialAccountsByUsername(String username) {
        UUID userId = userRepository.findByUsername(username).map(User::getId).orElseThrow(() -> new RuntimeException("User not found"));

        List<SocialAccount> accounts = socialAccountRepository.findByUserId(userId);

        return accounts.stream().map(this::mapToDto).toList();
    }

    public void deleteSocialAccountById(UUID id, String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

        SocialAccount account = socialAccountRepository.findById(id).orElseThrow(() -> new RuntimeException("Social account not found"));

        if (!account.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You are not allowed to delete this account");
        }

        socialAccountRepository.delete(account);
    }
}