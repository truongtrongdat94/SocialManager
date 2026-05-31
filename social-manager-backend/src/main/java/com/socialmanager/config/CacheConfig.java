package com.socialmanager.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration for Facebook API responses
 * 
 * NOTE: Currently using ConcurrentMapCacheManager (no TTL)
 * For production, consider using Caffeine with TTL:
 * 
 * <dependency>
 *     <groupId>com.github.ben-manes.caffeine</groupId>
 *     <artifactId>caffeine</artifactId>
 * </dependency>
 * 
 * Then replace with:
 * CaffeineCacheManager cacheManager = new CaffeineCacheManager();
 * cacheManager.setCaffeine(Caffeine.newBuilder()
 *     .expireAfterWrite(10, TimeUnit.MINUTES)
 *     .maximumSize(1000));
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        // Register cache names
        return new ConcurrentMapCacheManager(
            "pagePosts",      // Posts list cache
            "pageInsights",   // Page-level insights cache
            "postInsights"    // Post-level insights cache
        );
    }
}
