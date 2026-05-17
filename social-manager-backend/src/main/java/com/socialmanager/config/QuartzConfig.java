package com.socialmanager.config;

import com.socialmanager.job.AutoPostQuartzJob;
import com.socialmanager.job.TokenRefreshQuartzJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

import javax.sql.DataSource;

@Configuration
@Profile("!test")
public class QuartzConfig {

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(
            // DataSource dataSource,
            SpringBeanJobFactory springBeanJobFactory,
            JobDetail autoPostJobDetail,
            Trigger autoPostJobTrigger,
            JobDetail tokenRefreshJobDetail,
            Trigger tokenRefreshJobTrigger
    ) {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        // factory.setDataSource(dataSource);
        factory.setJobFactory(springBeanJobFactory);
        factory.setJobDetails(autoPostJobDetail, tokenRefreshJobDetail);
        factory.setTriggers(autoPostJobTrigger, tokenRefreshJobTrigger);
        factory.setOverwriteExistingJobs(true);
        factory.setWaitForJobsToCompleteOnShutdown(true);
        factory.setApplicationContextSchedulerContextKey("applicationContext");
        return factory;
    }

    @Bean
    public SpringBeanJobFactory springBeanJobFactory(ApplicationContext applicationContext) {
        AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        return jobFactory;
    }

    @Bean
    public JobDetail autoPostJobDetail() {
        return JobBuilder.newJob(AutoPostQuartzJob.class)
                .withIdentity("autoPostJob", "social-manager"
                )
                .withDescription("Publish scheduled social posts")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger autoPostJobTrigger(@Value("${app.posting.cron:0 */1 * * * ?}") String postingCron) {
        return TriggerBuilder.newTrigger()
                .forJob(autoPostJobDetail())
                .withIdentity("autoPostTrigger", "social-manager")
                .withSchedule(CronScheduleBuilder.cronSchedule(postingCron))
                .build();
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
        return TriggerBuilder.newTrigger()
                .forJob(tokenRefreshJobDetail())
                .withIdentity("tokenRefreshTrigger", "social-manager")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 2 * * ?"))
                .build();
    }

    private static final class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory {

        private AutowireCapableBeanFactory beanFactory;

        @Override
        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            this.beanFactory = applicationContext.getAutowireCapableBeanFactory();
        }

        @Override
        protected Object createJobInstance(org.quartz.spi.TriggerFiredBundle bundle) throws Exception {
            Object jobInstance = super.createJobInstance(bundle);
            beanFactory.autowireBean(jobInstance);
            return jobInstance;
        }
    }
}
