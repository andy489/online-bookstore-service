package com.bookstore.bookstore.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class TokenBlocklistService {

    private static final String PREFIX = "blocklist:";

    private final StringRedisTemplate redis;

    public TokenBlocklistService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void block(String jti, Date expiration) {
        // Bug #6 fix: null jti is a no-op — null-jti tokens are rejected by the filter (fail-closed)
        if (jti == null) return;
        long ttlSeconds = Duration.between(Instant.now(), expiration.toInstant()).getSeconds();
        // ttlSeconds <= 0 means the token is already expired — no need to block it,
        // since JwtTokenProvider.isTokenValid will reject it on its own.
        if (ttlSeconds > 0) {
            redis.opsForValue().set(PREFIX + jti, "1", Duration.ofSeconds(ttlSeconds));
        }
    }

    public boolean isBlocked(String jti) {
        // Bug #6 fix: null jti treated as blocked (fail-closed) — handled by caller
        if (jti == null) return true;
        return Boolean.TRUE.equals(redis.hasKey(PREFIX + jti));
    }
}
