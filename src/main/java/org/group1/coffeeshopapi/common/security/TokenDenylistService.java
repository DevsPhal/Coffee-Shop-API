package org.group1.coffeeshopapi.common.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

/**
 * Tracks revoked JWTs (by jti) in Redis so a logged-out token stops working
 * immediately instead of remaining valid until its natural expiry.
 */
@Service
public class TokenDenylistService {

    private static final String KEY_PREFIX = "denylist:";

    private final StringRedisTemplate redisTemplate;

    public TokenDenylistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void denylist(String jti, Date expiration) {
        if (jti == null || jti.isBlank() || expiration == null) {
            return;
        }
        long ttlMillis = expiration.getTime() - System.currentTimeMillis();
        if (ttlMillis <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(KEY_PREFIX + jti, "1", Duration.ofMillis(ttlMillis));
    }

    public boolean isDenylisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jti));
    }
}
