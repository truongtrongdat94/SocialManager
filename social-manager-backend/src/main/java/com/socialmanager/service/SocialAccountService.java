package com.socialmanager.service;

import com.socialmanager.dto.SocialAccountCreateRequest;
import com.socialmanager.dto.SocialAccountResponse;
import com.socialmanager.dto.SocialAccountUpdateRequest;
import com.socialmanager.exception.BusinessException;
import com.socialmanager.model.Platform;
import com.socialmanager.model.SocialAccount;
import com.socialmanager.repository.SocialAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialAccountService {

    private final SocialAccountRepository socialAccountRepository;
    private final TokenCryptoService tokenCryptoService;
    private final CurrentUserService currentUserService;

    public List<SocialAccountResponse> listAccounts() {
        UUID currentUserId = currentUserService.getCurrentUser().getId();
        return socialAccountRepository.findByUser_IdOrderByCreatedAtDesc(currentUserId)
                .stream()
                .map(SocialAccountResponse::from)
                .toList();
    }

    public SocialAccountResponse getAccount(UUID id) {
        return SocialAccountResponse.from(findAccountForCurrentUser(id));
    }

    @Transactional
    public SocialAccountResponse createAccount(SocialAccountCreateRequest request) {
        SocialAccount account = new SocialAccount();
        account.setUser(currentUserService.getCurrentUser());
        account.setPlatform(request.getPlatform());
        account.setExternalAccountId(request.getExternalAccountId());
        account.setAccountAlias(request.getAccountAlias());
        account.setAccountName(request.getAccountName());
        account.setProfilePictureUrl(request.getProfilePictureUrl());
        account.setAccessToken(tokenCryptoService.encrypt(request.getAccessToken().trim()));
        account.setRefreshToken(request.getRefreshToken());
        account.setExpiresAt(request.getExpiresAt());
        account.setScopes(request.getScopes());
        account.setIsAutoPilot(Boolean.TRUE.equals(request.getAutoPilot()));

        if (account.getAccountName() == null || account.getAccountName().isBlank()) {
            account.setAccountName(defaultAccountName(account.getPlatform()));
        }

        if (account.getAccountAlias() == null || account.getAccountAlias().isBlank()) {
            account.setAccountAlias(defaultAlias(account.getPlatform()));
        }

        return SocialAccountResponse.from(socialAccountRepository.save(account));
    }

    @Transactional
    public SocialAccountResponse updateAccount(UUID id, SocialAccountUpdateRequest request) {
        SocialAccount account = findAccountForCurrentUser(id);

        account.setPlatform(request.getPlatform());
        account.setExternalAccountId(request.getExternalAccountId());
        account.setAccountAlias(request.getAccountAlias());
        account.setAccountName(request.getAccountName());
        account.setProfilePictureUrl(request.getProfilePictureUrl());
        account.setRefreshToken(request.getRefreshToken());
        account.setExpiresAt(request.getExpiresAt());
        account.setScopes(request.getScopes());

        if (request.getAutoPilot() != null) {
            account.setIsAutoPilot(request.getAutoPilot());
        }

        if (request.getAccessToken() != null && !request.getAccessToken().isBlank()) {
            account.setAccessToken(tokenCryptoService.encrypt(request.getAccessToken().trim()));
        }

        return SocialAccountResponse.from(socialAccountRepository.save(account));
    }

    @Transactional
    public SocialAccountResponse toggleAutoPilot(UUID id, boolean enabled) {
        SocialAccount account = findAccountForCurrentUser(id);
        account.setIsAutoPilot(enabled);
        return SocialAccountResponse.from(socialAccountRepository.save(account));
    }

    @Transactional
    public void deleteAccount(UUID id) {
        SocialAccount account = findAccountForCurrentUser(id);
        socialAccountRepository.delete(account);
    }

    private SocialAccount findAccountForCurrentUser(UUID id) {
        UUID currentUserId = currentUserService.getCurrentUser().getId();
        return socialAccountRepository.findByIdAndUser_Id(id, currentUserId)
                .orElseThrow(() -> new BusinessException("Social Account not found"));
    }

    private String defaultAccountName(Platform platform) {
        return switch (platform) {
            case FACEBOOK -> "Facebook account";
            case INSTAGRAM -> "Instagram account";
            case THREADS -> "Threads account";
            case TIKTOK -> "TikTok account";
        };
    }

    private String defaultAlias(Platform platform) {
        return switch (platform) {
            case FACEBOOK -> "facebook";
            case INSTAGRAM -> "instagram";
            case THREADS -> "threads";
            case TIKTOK -> "tiktok";
        };
    }
}