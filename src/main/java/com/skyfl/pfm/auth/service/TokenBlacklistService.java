package com.skyfl.pfm.auth.service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {

    private final Optional<StringRedisTemplate> redisTemplate;
    private final Map<String, Long> fallback = new ConcurrentHashMap<>();

    public TokenBlacklistService(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.redisTemplate = Optional.ofNullable(redisTemplateProvider.getIfAvailable());
    }

    public void blacklist(String jti, Duration ttl) {
        if (jti == null) {
            return;
        }
        if (redisTemplate.isPresent()) {
            redisTemplate.get().opsForValue().set(key(jti), "1", ttl);
            return;
        }
        fallback.put(jti, System.currentTimeMillis() + ttl.toMillis());
    }

    public boolean isBlacklisted(String jti) {
        if (jti == null) {
            return false;
        }
        if (redisTemplate.isPresent()) {
            return Boolean.TRUE.equals(redisTemplate.get().hasKey(key(jti)));
        }
        Long expiresAt = fallback.get(jti);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt < System.currentTimeMillis()) {
            fallback.remove(jti);
            return false;
        }
        return true;
    }

    private String key(String jti) {
        return "blacklist:token:" + jti;
    }
}
