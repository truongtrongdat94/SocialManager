package com.socialmanager.job;

import com.socialmanager.repository.SocialAccountRepository;
import com.socialmanager.service.account.SocialAccountService;
import com.socialmanager.model.SocialAccount;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@DisallowConcurrentExecution
public class TokenRefreshQuartzJob extends QuartzJobBean {

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private SocialAccountService socialAccountService;

    @Override
    protected void executeInternal(@NonNull JobExecutionContext context) {
        // Tìm các tài khoản sắp hết hạn trong 7 ngày tới
        LocalDateTime threshold = LocalDateTime.now().plusDays(7);

        List<SocialAccount> accountsToRefresh = socialAccountRepository.findByExpiresAtBefore(threshold);

        for (SocialAccount account : accountsToRefresh) {
            try {
                socialAccountService.refreshAccessToken(account.getId());
            } catch (Exception e) {
                System.out.println("error:" + e.getMessage());
            }
        }
    }
}