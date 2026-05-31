package com.socialmanager.service;

import com.socialmanager.exception.ResourceNotFoundException;
import com.socialmanager.exception.UnauthorizedException;
import com.socialmanager.model.Platform;
import com.socialmanager.model.SocialAccount;
import com.socialmanager.model.User;
import com.socialmanager.repository.SocialAccountRepository;
import com.socialmanager.repository.UserRepository;
import com.socialmanager.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Shared service for Facebook token management
 * Prevents code duplication across PostService and InsightsService
 */
@Service
@RequiredArgsConstructor
public class FacebookTokenService {

    private final SocialAccountRepository socialAccountRepository;
    private final UserRepository userRepository;

    @Value("${AES_SECRET}")
    private String aesSecret;

    /**
     * Lấy page token từ DB và decrypt
     * Validates user ownership of the page
     * 
     * @param username User identifier (username or email)
     * @param pageId Facebook Page ID
     * @return Decrypted page access token
     * @throws ResourceNotFoundException if user not found
     * @throws UnauthorizedException if user doesn't own the page
     * @throws RuntimeException if decryption fails
     */
    public String getPageToken(String username, String pageId) {
        User user = userRepository.findByUsername(username)
            .or(() -> userRepository.findByEmail(username))
            .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại: " + username));

        SocialAccount account = socialAccountRepository
            .findByUserIdAndPlatformAndExternalAccountId(user.getId(), Platform.FACEBOOK, pageId)
            .orElseThrow(() -> new UnauthorizedException("Bạn không có quyền truy cập Page này hoặc Page không tồn tại"));

        try {
            return EncryptionUtil.decrypt(account.getAccessToken(), aesSecret);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi decrypt token. Vui lòng kết nối lại tài khoản Facebook.", e);
        }
    }
}
