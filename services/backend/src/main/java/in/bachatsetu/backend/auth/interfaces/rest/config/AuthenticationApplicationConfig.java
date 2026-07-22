package in.bachatsetu.backend.auth.interfaces.rest.config;

import in.bachatsetu.backend.auth.application.port.ClockPort;
import in.bachatsetu.backend.auth.application.port.HashingPort;
import in.bachatsetu.backend.auth.application.port.OtpEventPublisherPort;
import in.bachatsetu.backend.auth.application.port.OtpSenderPort;
import in.bachatsetu.backend.auth.application.port.RandomGeneratorPort;
import in.bachatsetu.backend.auth.application.port.RateLimiterPort;
import in.bachatsetu.backend.auth.application.service.GenerateOtpApplicationService;
import in.bachatsetu.backend.auth.application.service.InvalidateOtpApplicationService;
import in.bachatsetu.backend.auth.application.service.ResendOtpApplicationService;
import in.bachatsetu.backend.auth.application.service.VerifyOtpApplicationService;
import in.bachatsetu.backend.auth.application.usecase.GenerateOtpUseCase;
import in.bachatsetu.backend.auth.application.usecase.InvalidateOtpUseCase;
import in.bachatsetu.backend.auth.application.usecase.ResendOtpUseCase;
import in.bachatsetu.backend.auth.application.usecase.VerifyOtpUseCase;
import in.bachatsetu.backend.auth.application.validation.OtpRequestValidator;
import in.bachatsetu.backend.auth.domain.port.OtpVerificationRepository;
import in.bachatsetu.backend.auth.domain.port.UserRepository;
import in.bachatsetu.backend.auth.domain.service.OtpPolicyService;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composes framework-free OTP application services when all outbound ports exist.
 *
 * <p>Gated on the same {@code bachatsetu.persistence.repositories.enabled} property the outbound
 * adapters themselves use, rather than a cross-configuration-class {@code @ConditionalOnBean}
 * check: regular (non-auto-configuration) {@code @Configuration} classes discovered by component
 * scanning have no guaranteed processing order relative to one another, so a class-level
 * {@code @ConditionalOnBean} referencing ports defined by a sibling configuration class is
 * evaluated non-deterministically and can incorrectly skip this configuration even when every
 * required port is actually present.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AuthenticationApplicationConfig {

    @Bean
    public OtpRequestValidator otpRequestValidator(UserRepository userRepository) {
        return new OtpRequestValidator(userRepository);
    }

    @Bean
    public OtpPolicyService otpPolicyService() {
        return new OtpPolicyService();
    }

    @Bean
    public GenerateOtpUseCase generateOtpUseCase(
            OtpRequestValidator validator,
            OtpVerificationRepository repository,
            OtpPolicyService policyService,
            ClockPort clock,
            RandomGeneratorPort randomGenerator,
            HashingPort hashing,
            OtpSenderPort sender,
            OtpEventPublisherPort eventPublisher,
            RateLimiterPort rateLimiter,
            @Value("${bachatsetu.authentication.otp-rate-limit.max-attempts:5}") int rateLimitMaxAttempts,
            @Value("${bachatsetu.authentication.otp-rate-limit.window:1m}") Duration rateLimitWindow) {
        return new GenerateOtpApplicationService(
                validator, repository, policyService, clock, randomGenerator, hashing, sender, eventPublisher,
                rateLimiter, rateLimitMaxAttempts, rateLimitWindow);
    }

    @Bean
    public VerifyOtpUseCase verifyOtpUseCase(
            OtpRequestValidator validator,
            OtpVerificationRepository repository,
            ClockPort clock,
            HashingPort hashing,
            OtpEventPublisherPort eventPublisher) {
        return new VerifyOtpApplicationService(validator, repository, clock, hashing, eventPublisher);
    }

    @Bean
    public ResendOtpUseCase resendOtpUseCase(
            OtpRequestValidator validator,
            OtpVerificationRepository repository,
            OtpPolicyService policyService,
            ClockPort clock,
            RandomGeneratorPort randomGenerator,
            HashingPort hashing,
            OtpSenderPort sender,
            OtpEventPublisherPort eventPublisher) {
        return new ResendOtpApplicationService(
                validator, repository, policyService, clock, randomGenerator, hashing, sender, eventPublisher);
    }

    @Bean
    public InvalidateOtpUseCase invalidateOtpUseCase(
            OtpRequestValidator validator,
            OtpVerificationRepository repository,
            ClockPort clock) {
        return new InvalidateOtpApplicationService(validator, repository, clock);
    }
}
