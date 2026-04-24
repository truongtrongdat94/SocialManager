package com.socialmanager.config;

import com.socialmanager.job.AutoPostQuartzJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.sql.DataSource;
import java.util.Arrays;

@Configuration
@Profile("!test")
public class QuartzConfig {

    @Value("${app.posting.cron:0 */1 * * * ?}")
    private String autoPostCron;

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Autowired
    private Environment environment;

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(DataSource dataSource, JobDetail autoPostJobDetail, Trigger autoPostTrigger) {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        // In local profile we keep Quartz in-memory to avoid requiring external DB infra.
        boolean localProfileActive = Arrays.asList(environment.getActiveProfiles()).contains("local");
        if (!localProfileActive) {
            factory.setDataSource(dataSource);
        }
        factory.setJobDetails(autoPostJobDetail);
        factory.setTriggers(autoPostTrigger);
        factory.setJobFactory(springBeanJobFactory());
        factory.setApplicationContextSchedulerContextKey("applicationContext");
        factory.setWaitForJobsToCompleteOnShutdown(true);
        factory.setOverwriteExistingJobs(true);
        return factory;
    }

    @Bean
    public JobDetail autoPostJobDetail() {
        return JobBuilder.newJob(AutoPostQuartzJob.class)
                .withIdentity("autoPostJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger autoPostTrigger(JobDetail autoPostJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(autoPostJobDetail)
                .withIdentity("autoPostTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(autoPostCron)
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    @Bean
    public SpringBeanJobFactory springBeanJobFactory() {
        return new SpringBeanJobFactory() {
            @Override
            protected Object createJobInstance(org.quartz.spi.TriggerFiredBundle bundle) throws Exception {
                Object job = super.createJobInstance(bundle);
                beanFactory.autowireBean(job);
                return job;
            }
        };
    }
}
