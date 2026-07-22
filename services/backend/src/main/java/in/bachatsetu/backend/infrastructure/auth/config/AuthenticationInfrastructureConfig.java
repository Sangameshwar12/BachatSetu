package in.bachatsetu.backend.infrastructure.auth.config;

import in.bachatsetu.backend.auth.application.port.ClockPort;
import in.bachatsetu.backend.auth.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.auth.application.port.HashingPort;
import in.bachatsetu.backend.auth.application.port.OtpEventPublisherPort;
import in.bachatsetu.backend.auth.application.port.PasswordHashGeneratorPort;
import in.bachatsetu.backend.auth.application.port.RandomGeneratorPort;
import in.bachatsetu.backend.auth.application.port.RateLimiterPort;
import in.bachatsetu.backend.infrastructure.auth.adapter.ApplicationEventDomainEventPublisherAdapter;
import in.bachatsetu.backend.infrastructure.auth.adapter.ApplicationEventOtpPublisherAdapter;
import in.bachatsetu.backend.infrastructure.auth.adapter.BCryptHashingAdapter;
import in.bachatsetu.backend.infrastructure.auth.adapter.FixedTestOtpGeneratorAdapter;
import in.bachatsetu.backend.infrastructure.auth.adapter.RandomPasswordHashGeneratorAdapter;
import in.bachatsetu.backend.infrastructure.auth.adapter.RedisRateLimiterAdapter;
import in.bachatsetu.backend.infrastructure.auth.adapter.SecureRandomGeneratorAdapter;
import in.bachatsetu.backend.infrastructure.auth.adapter.SystemClockAdapter;
import java.security.SecureRandom;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder.BCryptVersion;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuthenticationProperties.class)
public class AuthenticationInfrastructureConfig {

    @Bean
    Clock authenticationClock() {
        return Clock.systemUTC();
    }

    @Bean
    SecureRandom authenticationSecureRandom() {
        return new SecureRandom();
    }

    @Bean
    BCryptPasswordEncoder otpPasswordEncoder(
            AuthenticationProperties properties,
            SecureRandom authenticationSecureRandom) {
        return new BCryptPasswordEncoder(
                BCryptVersion.$2A, properties.hashStrength(), authenticationSecureRandom);
    }

    @Bean
    ClockPort systemClockAdapter(Clock authenticationClock) {
        return new SystemClockAdapter(authenticationClock);
    }

    /**
     * Active whenever {@code bachatsetu.authentication.otp.test-mode} ({@code
     * AUTH_OTP_TEST_MODE}) is {@code false} or unset — the default for every Spring profile,
     * including {@code prod}. {@link #fixedTestOtpGeneratorAdapter()} is active under the exact
     * opposite condition, so precisely one {@link RandomGeneratorPort} bean exists no matter which
     * Spring profile is running or whether the flag was set at all.
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "bachatsetu.authentication.otp",
            name = "test-mode",
            havingValue = "false",
            matchIfMissing = true)
    RandomGeneratorPort secureRandomGeneratorAdapter(SecureRandom authenticationSecureRandom) {
        return new SecureRandomGeneratorAdapter(authenticationSecureRandom);
    }

    // TEMPORARY MVP TEST OTP — REMOVE BEFORE PRODUCTION
    // MVP/demo-only: every OTP becomes the fixed code 102030 so signup/login/reset can be
    // exercised without a real Email/SMS provider. See FixedTestOtpGeneratorAdapter.
    @Bean
    @ConditionalOnProperty(prefix = "bachatsetu.authentication.otp", name = "test-mode", havingValue = "true")
    RandomGeneratorPort fixedTestOtpGeneratorAdapter() {
        return new FixedTestOtpGeneratorAdapter();
    }

    @Bean
    HashingPort bcryptHashingAdapter(BCryptPasswordEncoder otpPasswordEncoder) {
        return new BCryptHashingAdapter(otpPasswordEncoder);
    }

    @Bean
    OtpEventPublisherPort applicationEventOtpPublisherAdapter(ApplicationEventPublisher publisher) {
        return new ApplicationEventOtpPublisherAdapter(publisher);
    }

    @Bean
    PasswordHashGeneratorPort randomPasswordHashGeneratorAdapter(
            BCryptPasswordEncoder otpPasswordEncoder, SecureRandom authenticationSecureRandom) {
        return new RandomPasswordHashGeneratorAdapter(otpPasswordEncoder, authenticationSecureRandom);
    }

    @Bean
    DomainEventPublisherPort authApplicationEventDomainEventPublisherAdapter(ApplicationEventPublisher publisher) {
        return new ApplicationEventDomainEventPublisherAdapter(publisher);
    }

    /**
     * Gated on {@code bachatsetu.cache.enabled} (default {@code true}) — the same flag several
     * minimal-context tests already set to {@code false} to exclude Redis auto-configuration
     * entirely (see {@code HealthEndpointTest}, {@code SecurityIntegrationTest}, and similar).
     * Without this guard, {@link StringRedisTemplate} would be unavailable in those contexts and
     * this bean's creation would fail outright.
     */
    @Bean
    @ConditionalOnProperty(prefix = "bachatsetu.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
    RateLimiterPort redisRateLimiterAdapter(StringRedisTemplate redisTemplate) {
        return new RedisRateLimiterAdapter(redisTemplate);
    }
}
