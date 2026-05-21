package com.socialmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SocialManagerBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SocialManagerBackendApplication.class, args);
    }
}