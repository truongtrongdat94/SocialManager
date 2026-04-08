package com.socialmanager.scheduler;

import com.socialmanager.model.SocialAccount;
import com.socialmanager.repository.SocialAccountRepository;
import com.socialmanager.util.EncryptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@DisallowConcurrentExecution
public class TokenRefreshQuartzJob extends QuartzJobBean {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        try {
            // 1. Lấy ApplicationContext từ cấu hình mà bạn của bạn đã thiết lập trong QuartzConfig
            ApplicationContext appCtx = (ApplicationContext) context.getScheduler()
                    .getContext().get("applicationContext");

            // 2. Lấy các Bean cần thiết từ Spring Context
            SocialAccountRepository repository = appCtx.getBean(SocialAccountRepository.class);

            // Lấy Secret Key và Meta Config từ Environment (thông qua AppCtx)
            String aesSecret = appCtx.getEnvironment().getProperty("AES_SECRET");
            String metaAppId = appCtx.getEnvironment().getProperty("META_APP_ID");
            String metaAppSecret = appCtx.getEnvironment().getProperty("META_APP_SECRET");

            // 3. Tìm các tài khoản sắp hết hạn (ví dụ: trong vòng 7 ngày tới)
            LocalDateTime threshold = LocalDateTime.now().plusDays(7);
            List<SocialAccount> accountsToRefresh = repository.findByExpiresAtBefore(threshold);

            log.info("Tìm thấy {} tài khoản cần làm mới token.", accountsToRefresh.size());

            for (SocialAccount account : accountsToRefresh) {
                try {
                    // Giải mã token cũ
                    assert aesSecret != null;
                    String decryptedOldToken = EncryptionUtil.decrypt(account.getAccessToken(), aesSecret);

                    // Logic đổi token cho Meta (Facebook/Instagram)
                    if (account.getPlatform().name().startsWith("FACEBOOK") ||
                            account.getPlatform().name().startsWith("INSTAGRAM")) {

                        String fbUrl = String.format(
                                "https://graph.facebook.com/v19.0/oauth/access_token?grant_type=fb_exchange_token&client_id=%s&client_secret=%s&fb_exchange_token=%s",
                                metaAppId, metaAppSecret, decryptedOldToken);

                        ResponseEntity<Map<String, Object>> responseEntity =
                                restTemplate.exchange(
                                        fbUrl,
                                        HttpMethod.GET,
                                        null,
                                        new ParameterizedTypeReference<>() {
                                        }
                                );

                        Map<String, Object> response = responseEntity.getBody();

                        if (response != null && response.containsKey("access_token")) {
                            String newAccessToken = (String) response.get("access_token");
                            long expiresIn = ((Number) response.getOrDefault("expires_in", 5184000L)).longValue();

                            // Mã hóa và lưu token mới
                            account.setAccessToken(EncryptionUtil.encrypt(newAccessToken, aesSecret));
                            account.setExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));
                            repository.save(account);

                            log.info("Đã làm mới token thành công cho: {}", account.getAccountName());
                        }
                    }
                    // Thêm logic cho TikTok tương tự ở đây...

                } catch (Exception e) {
                    log.error("Lỗi khi xử lý account {}: {}", account.getAccountName(), e.getMessage());
                }
            }

        } catch (SchedulerException e) {
            log.error("Không thể truy cập ApplicationContext từ Quartz", e);
            throw new JobExecutionException(e);
        }

        log.info("--- Quartz Job: Hoàn thành ---");
    }
}