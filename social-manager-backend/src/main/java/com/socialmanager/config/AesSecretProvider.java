package com.socialmanager.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import org.springframework.beans.factory.InitializingBean;

@Component
public class AesSecretProvider implements InitializingBean {

    private static final Path SECRET_FILE = Paths.get(System.getProperty("user.home"), ".social-manager", "aes-secret.txt");

    @Value("${AES_SECRET:}")
    private String configured;

    private String secret;

    public String getSecret() {
        return secret;
    }

    @Override
    public void afterPropertiesSet() {
        if (configured != null && !configured.trim().isEmpty()) {
            secret = configured.trim();
            return;
        }

        String persisted = readPersistedSecret();
        if (persisted != null && !persisted.trim().isEmpty()) {
            secret = persisted.trim();
            return;
        }

        // Generate a cryptographically secure 32-character ASCII secret (32 bytes)
        SecureRandom rnd = new SecureRandom();
        final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < 32; i++) {
            sb.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
        }
        secret = sb.toString();
        persistSecret(secret);
        System.out.println("[WARN] AES_SECRET not provided; generated and persisted a local AES secret at " + SECRET_FILE + ".");
    }

    private String readPersistedSecret() {
        try {
            if (Files.exists(SECRET_FILE)) {
                return Files.readString(SECRET_FILE, StandardCharsets.UTF_8).trim();
            }
        } catch (IOException ignored) {
            // Fall through to regeneration.
        }
        return null;
    }

    private void persistSecret(String generatedSecret) {
        try {
            Files.createDirectories(SECRET_FILE.getParent());
            Files.writeString(SECRET_FILE, generatedSecret, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Không thể lưu AES secret cục bộ", ex);
        }
    }
}
