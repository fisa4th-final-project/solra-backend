package com.fisa.solra.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CacheConfig {

    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(30)) // 30분 뒤 만료
                .maximumSize(10);                         // 최대 10개 캐시
    }

    @Bean
    public CacheManager cacheManager(Caffeine<Object, Object> caffeine) {
        CaffeineCacheManager cm = new CaffeineCacheManager("k8sClient");
        cm.setCaffeine(caffeine);
        return cm;
    }
}