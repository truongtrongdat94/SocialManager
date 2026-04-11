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

@Configuration
@Profile("!test")
public class QuartzConfig {

    /* Cái này set thủ công nó bị xung đột gì á không biết, xoá đi để nó auto thì ổn
    import org.springframework.scheduling.quartz.SchedulerFactoryBean;
    import javax.sql.DataSource;

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(DataSource dataSource, Trigger tokenRefreshJobTrigger, JobDetail tokenRefreshJobDetail) {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setApplicationContextSchedulerContextKey("applicationContext");
        factory.setWaitForJobsToCompleteOnShutdown(true);
        factory.setOverwriteExistingJobs(true);
        factory.setJobDetails(tokenRefreshJobDetail);
        factory.setTriggers(tokenRefreshJobTrigger);
        return factory;
    }
    */

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
        // Test: "0/20 * * * * ?" (20 giây 1 lần)
        // Prod: "0 0 2 * * ?" (2h sáng mỗi ngày)
        return TriggerBuilder.newTrigger()
            .forJob(tokenRefreshJobDetail())
            .withIdentity("tokenRefreshTrigger", "social-manager")
            .withSchedule(CronScheduleBuilder.cronSchedule("0/20 * * * * ?"))
            .build();
    }
}
