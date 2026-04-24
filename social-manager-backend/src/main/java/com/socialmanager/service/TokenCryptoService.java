package com.socialmanager.service;

import com.socialmanager.exception.BusinessException;
import com.socialmanager.util.EncryptionUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenCryptoService {

    @Value("${app.aes-secret}")
    private String aesSecret;

    @PostConstruct
    void validateKey() {
        int length = aesSecret != null ? aesSecret.length() : 0;
        if (length != 16 && length != 24 && length != 32) {
            throw new IllegalStateException("app.aes-secret must be 16, 24, or 32 characters long");
        }
    }

    public String decrypt(String encryptedToken) {
        try {
            return EncryptionUtil.decrypt(encryptedToken, aesSecret);
        } catch (Exception ex) {
            throw new BusinessException("Failed to decrypt social account token");
        }
    }

    public String encrypt(String token) {
        try {
            return EncryptionUtil.encrypt(token, aesSecret);
        } catch (Exception ex) {
            throw new BusinessException("Failed to encrypt social account token");
        }
    }
}
