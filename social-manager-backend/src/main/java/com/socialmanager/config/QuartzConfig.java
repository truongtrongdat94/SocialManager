package com.socialmanager.config;

import com.socialmanager.job.TokenRefreshQuartzJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.quartz.Trigger;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.sql.DataSource;

@Configuration
@Profile("!test")
public class QuartzConfig {

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(DataSource dataSource) {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setApplicationContextSchedulerContextKey("applicationContext");
        factory.setWaitForJobsToCompleteOnShutdown(true);
        factory.setOverwriteExistingJobs(true);
        return factory;
    }

    @Bean
    public JobDetail tokenRefreshJobDetail() {
        return JobBuilder.newJob(TokenRefreshQuartzJob.class)
                .withIdentity("tokenRefreshJob", "social-manager")
                .withDescription("Refresh OAuth tokens for all platforms")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger tokenRefreshJobTrigger() {
        // Chạy vào lúc 2:00 sáng mỗi ngày
        return TriggerBuilder.newTrigger()
                .forJob(tokenRefreshJobDetail())
                .withIdentity("tokenRefreshTrigger", "social-manager")
                .withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(2, 0))
                .build();
    }
}
