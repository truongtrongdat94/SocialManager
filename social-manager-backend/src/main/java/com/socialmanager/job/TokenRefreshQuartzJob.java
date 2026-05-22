package com.socialmanager.job;

import com.socialmanager.repository.SocialAccountRepository;
import com.socialmanager.service.SocialAccountService;
import com.socialmanager.model.SocialAccount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.quartz.*;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class TokenRefreshQuartzJob extends QuartzJobBean {

    private final SocialAccountRepository socialAccountRepository;
    private final SocialAccountService socialAccountService;

    @Override
    protected void executeInternal(@NonNull JobExecutionContext context) {
        // Tìm các tài khoản sắp hết hạn trong 7 ngày tới
        LocalDateTime threshold = LocalDateTime.now().plusDays(7);

        List<SocialAccount> accountsToRefresh = socialAccountRepository.findByExpiresAtBefore(threshold);

        for (SocialAccount account : accountsToRefresh) {
            try {
                socialAccountService.refreshAccessToken(account.getId());
            } catch (Exception e) {
                // Token refresh failed - will retry on next scheduled run
            }
        }
    }
}