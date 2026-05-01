package com.socialmanager.util;

import com.socialmanager.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption utility for storing OAuth tokens at rest.
 * Output format: Base64(iv[12 bytes] + ciphertext + authTag[16 bytes])
 * 
 * Injected as a Spring Component to validate AES secret on startup.
 */
@Component
public class EncryptionUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128; // bits

    @Value("${app.aes-secret}")
    private String aesSecret;

    @PostConstruct
    void validateKey() {
        int length = aesSecret != null ? aesSecret.length() : 0;
        if (length != 16 && length != 24 && length != 32) {
            throw new IllegalStateException("app.aes-secret must be 16, 24, or 32 characters long");
        }
    }

    /**
     * Encrypt using injected AES secret
     */
    public String encrypt(String data) {
        try {
            return encryptWithKey(data, aesSecret);
        } catch (Exception ex) {
            throw new BusinessException("Failed to encrypt data");
        }
    }

    /**
     * Decrypt using injected AES secret
     */
    public String decrypt(String encryptedData) {
        try {
            return decryptWithKey(encryptedData, aesSecret);
        } catch (Exception ex) {
            throw new BusinessException("Failed to decrypt data");
        }
    }

    // Static methods for backward compatibility if used elsewhere
    public static String encrypt(String data, String secretKey) throws Exception {
        return encryptWithKey(data, secretKey);
    }

    public static String decrypt(String encryptedData, String secretKey) throws Exception {
        return decryptWithKey(encryptedData, secretKey);
    }

    private static String encryptWithKey(String data, String secretKey) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(secretKey.getBytes(), "AES"),
                new GCMParameterSpec(GCM_TAG_LENGTH, iv));

        byte[] encrypted = cipher.doFinal(data.getBytes());

        // Prepend IV to ciphertext
        byte[] result = new byte[GCM_IV_LENGTH + encrypted.length];
        System.arraycopy(iv, 0, result, 0, GCM_IV_LENGTH);
        System.arraycopy(encrypted, 0, result, GCM_IV_LENGTH, encrypted.length);

        return Base64.getEncoder().encodeToString(result);
    }

    private static String decryptWithKey(String encryptedData, String secretKey) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(encryptedData);

        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(decoded, 0, iv, 0, GCM_IV_LENGTH);

        byte[] ciphertext = new byte[decoded.length - GCM_IV_LENGTH];
        System.arraycopy(decoded, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE,
                new SecretKeySpec(secretKey.getBytes(), "AES"),
                new GCMParameterSpec(GCM_TAG_LENGTH, iv));

        return new String(cipher.doFinal(ciphertext));
    }
}
