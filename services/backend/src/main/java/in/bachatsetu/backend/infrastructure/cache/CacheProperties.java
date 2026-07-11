package in.bachatsetu.backend.infrastructure.cache;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Time-to-live configuration for each named Redis cache region. */
@ConfigurationProperties(prefix = "bachatsetu.cache")
public record CacheProperties(
        boolean enabled, Duration otpTtl, Duration rateLimitTtl, Duration sessionTtl, Duration configTtl) {

    public CacheProperties {
        Objects.requireNonNull(otpTtl, "otp cache TTL must not be null");
        Objects.requireNonNull(rateLimitTtl, "rate limit cache TTL must not be null");
        Objects.requireNonNull(sessionTtl, "session cache TTL must not be null");
        Objects.requireNonNull(configTtl, "config cache TTL must not be null");
        if (otpTtl.isNegative() || rateLimitTtl.isNegative() || sessionTtl.isNegative() || configTtl.isNegative()) {
            throw new IllegalArgumentException("cache time-to-live values must not be negative");
        }
    }
}
