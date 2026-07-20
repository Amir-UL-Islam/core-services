package com.central.security.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine-backed in-memory cache configuration.
 *
 * Cache names and their TTL strategy:
 *  - "privileges": privilege→URL mapping loaded on every ACL check.
 *                       TTL=5min, max 1 entry (the full list). Evicted
 *                       eagerly on any privilege/URL writing.
 *  - "roleHierarchy": role parent-chain string for Spring Security.
 *                       TTL=10min. Evicted on role hierarchy changes.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_PRIVILEGES    = "privileges";
    public static final String CACHE_ROLE_HIERARCHY = "roleHierarchy";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                CACHE_PRIVILEGES, CACHE_ROLE_HIERARCHY);
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(500));
        return manager;
    }
}
