package in.bachatsetu.backend.infrastructure.auth.adapter;

import in.bachatsetu.backend.auth.application.port.RateLimiterPort;
import java.time.Duration;
import java.util.Objects;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Fixed-window request counter backed by Redis {@code INCR}/{@code EXPIRE} — atomic under
 * concurrent requests, unlike a get-then-put through the Spring {@code Cache} abstraction (the
 * {@code rate-limit} cache region {@link in.bachatsetu.backend.infrastructure.cache.CacheConfiguration}
 * declares has no atomic increment operation, so this adapter talks to Redis directly instead).
 */
public final class RedisRateLimiterAdapter implements RateLimiterPort {

    private static final String KEY_PREFIX = "rate-limit:";

    private final StringRedisTemplate redisTemplate;

    public RedisRateLimiterAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
    }

    @Override
    public boolean tryConsume(String key, int maxAttempts, Duration window) {
        Objects.requireNonNull(key, "key must not be null");
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("maxAttempts must be positive");
        }
        Objects.requireNonNull(window, "window must not be null");
        String redisKey = KEY_PREFIX + key;
        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count != null && count == 1L) {
            redisTemplate.expire(redisKey, window);
        }
        return count == null || count <= maxAttempts;
    }
}
