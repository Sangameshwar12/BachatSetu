package in.bachatsetu.backend.auth.interfaces.rest.config;

import in.bachatsetu.backend.auth.application.login.service.CompleteLoginApplicationService;
import in.bachatsetu.backend.auth.application.login.service.StartLoginApplicationService;
import in.bachatsetu.backend.auth.application.login.usecase.CompleteLoginUseCase;
import in.bachatsetu.backend.auth.application.login.usecase.StartLoginUseCase;
import in.bachatsetu.backend.auth.application.token.usecase.GenerateAccessTokenUseCase;
import in.bachatsetu.backend.auth.application.token.usecase.GenerateRefreshTokenUseCase;
import in.bachatsetu.backend.auth.application.usecase.GenerateOtpUseCase;
import in.bachatsetu.backend.auth.application.usecase.VerifyOtpUseCase;
import in.bachatsetu.backend.auth.domain.port.TenantProvider;
import in.bachatsetu.backend.auth.domain.port.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composes the framework-free login (returning-user sign-in) application services on top of the
 * already-wired OTP and token subsystems — the exact same beans {@link SignupApplicationConfig}
 * composes for signup, reused rather than duplicated.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class LoginApplicationConfig {

    @Bean
    @ConditionalOnProperty(prefix = "bachatsetu.authentication.token", name = "enabled", havingValue = "true")
    public StartLoginUseCase startLoginUseCase(UserRepository authUserRepository, GenerateOtpUseCase generateOtp) {
        return new StartLoginApplicationService(authUserRepository, generateOtp);
    }

    @Bean
    @ConditionalOnProperty(prefix = "bachatsetu.authentication.token", name = "enabled", havingValue = "true")
    public CompleteLoginUseCase completeLoginUseCase(
            VerifyOtpUseCase verifyOtp,
            UserRepository authUserRepository,
            GenerateAccessTokenUseCase generateAccessToken,
            GenerateRefreshTokenUseCase generateRefreshToken,
            TenantProvider tenantProvider) {
        return new CompleteLoginApplicationService(
                verifyOtp, authUserRepository, generateAccessToken, generateRefreshToken, tenantProvider);
    }
}
