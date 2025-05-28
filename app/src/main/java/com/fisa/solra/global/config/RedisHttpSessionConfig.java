package com.fisa.solra.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@Configuration
@EnableRedisHttpSession // 🔥 Redis에 세션 저장을 활성화하는 핵심 어노테이션
public class RedisHttpSessionConfig {
}
