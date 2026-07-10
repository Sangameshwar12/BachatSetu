package in.bachatsetu.backend.auth.interfaces.rest.config;

import in.bachatsetu.backend.auth.application.port.ClockPort;
import in.bachatsetu.backend.auth.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.auth.application.port.PasswordHashGeneratorPort;
import in.bachatsetu.backend.auth.application.signup.service.CompleteSignupApplicationService;
import in.bachatsetu.backend.auth.application.signup.service.StartSignupApplicationService;
import in.bachatsetu.backend.auth.application.signup.usecase.CompleteSignupUseCase;
import in.bachatsetu.backend.auth.application.signup.usecase.StartSignupUseCase;
import in.bachatsetu.backend.auth.application.token.usecase.GenerateAccessTokenUseCase;
import in.bachatsetu.backend.auth.application.token.usecase.GenerateRefreshTokenUseCase;
import in.bachatsetu.backend.auth.application.usecase.GenerateOtpUseCase;
import in.bachatsetu.backend.auth.application.usecase.VerifyOtpUseCase;
import in.bachatsetu.backend.auth.domain.port.ProfileProvisioningPort;
import in.bachatsetu.backend.auth.domain.port.RoleRepository;
import in.bachatsetu.backend.auth.domain.port.TenantProvider;
import in.bachatsetu.backend.auth.domain.port.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composes the framework-free signup application services on top of the already-wired OTP and
 * token subsystems. Gated on the same persistence and token-issuance flags those subsystems use,
 * since signup cannot function without both.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class SignupApplicationConfig {

    @Bean
    @ConditionalOnProperty(prefix = "bachatsetu.authentication.token", name = "enabled", havingValue = "true")
    public StartSignupUseCase startSignupUseCase(
            ProfileProvisioningPort profileProvisioning,
            UserRepository authUserRepository,
            GenerateOtpUseCase generateOtp,
            PasswordHashGeneratorPort passwordHashGenerator,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock) {
        return new StartSignupApplicationService(
                profileProvisioning, authUserRepository, generateOtp, passwordHashGenerator, eventPublisher,
                clock);
    }

    @Bean
    @ConditionalOnProperty(prefix = "bachatsetu.authentication.token", name = "enabled", havingValue = "true")
    public CompleteSignupUseCase completeSignupUseCase(
            VerifyOtpUseCase verifyOtp,
            UserRepository authUserRepository,
            ProfileProvisioningPort profileProvisioning,
            RoleRepository roleRepository,
            GenerateAccessTokenUseCase generateAccessToken,
            GenerateRefreshTokenUseCase generateRefreshToken,
            TenantProvider tenantProvider,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock) {
        return new CompleteSignupApplicationService(
                verifyOtp, authUserRepository, profileProvisioning, roleRepository, generateAccessToken,
                generateRefreshToken, tenantProvider, eventPublisher, clock);
    }
}
