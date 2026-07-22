package in.bachatsetu.backend.infrastructure.auth.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisRateLimiterAdapterTest {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    @Test
    void allowsTheFirstAttemptAndSetsTheWindowExpiry() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("rate-limit:otp-generate:+919812345678")).thenReturn(1L);
        RedisRateLimiterAdapter adapter = new RedisRateLimiterAdapter(redisTemplate);

        boolean allowed = adapter.tryConsume("otp-generate:+919812345678", 5, WINDOW);

        assertThat(allowed).isTrue();
        verify(redisTemplate).expire("rate-limit:otp-generate:+919812345678", WINDOW);
    }

    @Test
    void allowsSubsequentAttemptsWithinTheLimitWithoutResettingExpiry() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("rate-limit:otp-generate:+919812345678")).thenReturn(3L);
        RedisRateLimiterAdapter adapter = new RedisRateLimiterAdapter(redisTemplate);

        boolean allowed = adapter.tryConsume("otp-generate:+919812345678", 5, WINDOW);

        assertThat(allowed).isTrue();
        verify(redisTemplate, never()).expire(anyString(), any());
    }

    @Test
    void rejectsOnceTheCountExceedsMaxAttempts() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("rate-limit:otp-generate:+919812345678")).thenReturn(6L);
        RedisRateLimiterAdapter adapter = new RedisRateLimiterAdapter(redisTemplate);

        boolean allowed = adapter.tryConsume("otp-generate:+919812345678", 5, WINDOW);

        assertThat(allowed).isFalse();
    }

    @Test
    void allowsTheRequestIfRedisReturnsNoCount() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("rate-limit:otp-generate:+919812345678")).thenReturn(null);
        RedisRateLimiterAdapter adapter = new RedisRateLimiterAdapter(redisTemplate);

        boolean allowed = adapter.tryConsume("otp-generate:+919812345678", 5, WINDOW);

        assertThat(allowed).isTrue();
    }

    @Test
    void rejectsNonPositiveMaxAttempts() {
        RedisRateLimiterAdapter adapter = new RedisRateLimiterAdapter(mock(StringRedisTemplate.class));

        assertThatThrownBy(() -> adapter.tryConsume("key", 0, WINDOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullConstructorArgument() {
        assertThatThrownBy(() -> new RedisRateLimiterAdapter(null)).isInstanceOf(NullPointerException.class);
    }
}
