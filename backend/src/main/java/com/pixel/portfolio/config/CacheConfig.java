package com.pixel.portfolio.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Each Finnhub-backed cache gets its own TTL so quotes refresh often while
 * profiles (rarely-changing company data) stay cached far longer — keeps us
 * comfortably under Finnhub's 60 req/min free-tier limit.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                buildCache("quotes", 30, TimeUnit.SECONDS, 500),
                buildCache("profiles", 24, TimeUnit.HOURS, 500),
                buildCache("news", 10, TimeUnit.MINUTES, 200),
                buildCache("search", 5, TimeUnit.MINUTES, 200)
        ));
        return manager;
    }

    private CaffeineCache buildCache(String name, long duration, TimeUnit unit, int maxSize) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .expireAfterWrite(duration, unit)
                .maximumSize(maxSize)
                .build());
    }
}

