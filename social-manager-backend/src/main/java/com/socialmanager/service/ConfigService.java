package com.socialmanager.service;

import com.socialmanager.model.AppConfig;
import com.socialmanager.repository.AppConfigRepository;
import com.socialmanager.config.AesSecretProvider;
import com.socialmanager.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConfigService {
    private final AppConfigRepository appConfigRepository;
    private final AesSecretProvider aesSecretProvider;

    private static final String META_APP_ID_KEY = "meta.app-id";
    private static final String META_APP_SECRET_KEY = "meta.app-secret";
    private static final String META_REDIRECT_URI_KEY = "meta.redirect-uri";

    public Optional<String> getRaw(String key) {
        return appConfigRepository.findByKey(key).map(AppConfig::getValue);
    }

    public Optional<String> getMetaAppId() {
        return getRaw(META_APP_ID_KEY);
    }

    public Optional<String> getMetaRedirectUri() {
        return getRaw(META_REDIRECT_URI_KEY);
    }

    public Optional<String> getMetaAppSecretDecrypted() {
        return getRaw(META_APP_SECRET_KEY).map(v -> {
            try {
                return EncryptionUtil.decrypt(v, aesSecretProvider.getSecret());
            } catch (Exception e) {
                throw new RuntimeException("Failed to decrypt stored meta app secret", e);
            }
        });
    }

    public void setMetaConfig(String appId, String appSecretPlain, String redirectUri) {
        saveOrUpdate(META_APP_ID_KEY, appId);
        saveOrUpdate(META_REDIRECT_URI_KEY, redirectUri);

        if (appSecretPlain != null) {
            try {
                String encrypted = EncryptionUtil.encrypt(appSecretPlain, aesSecretProvider.getSecret());
                saveOrUpdate(META_APP_SECRET_KEY, encrypted);
            } catch (Exception e) {
                throw new RuntimeException("Failed to encrypt meta app secret", e);
            }
        }
    }

    private void saveOrUpdate(String key, String value) {
        if (value == null) return;
        Optional<AppConfig> existing = appConfigRepository.findByKey(key);
        if (existing.isPresent()) {
            AppConfig ac = existing.get();
            ac.setValue(value);
            appConfigRepository.save(ac);
        } else {
            AppConfig ac = AppConfig.builder().key(key).value(value).build();
            appConfigRepository.save(ac);
        }
    }
}
