package com.damir.cache;

import com.damir.cache.core.LruCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Bean
    public LruCache<String, String> lruCache(
            @Value("${cache.capacity:1000}") int capacity,
            @Value("${cache.sweep-interval-seconds:30}") long sweepIntervalSeconds) {
        return new LruCache<>(capacity, sweepIntervalSeconds);
    }
}
