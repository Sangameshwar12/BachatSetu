package in.bachatsetu.backend.configuration.auth;

import in.bachatsetu.backend.auth.application.token.port.JwtProviderPort;
import in.bachatsetu.backend.auth.application.token.port.TokenClockPort;
import in.bachatsetu.backend.auth.application.token.port.TokenHasherPort;
import in.bachatsetu.backend.auth.application.token.service.GenerateAccessTokenApplicationService;
import in.bachatsetu.backend.auth.application.token.service.GenerateRefreshTokenApplicationService;
import in.bachatsetu.backend.auth.application.token.service.RefreshAccessTokenApplicationService;
import in.bachatsetu.backend.auth.application.token.service.RefreshTokenCredentialVerifier;
import in.bachatsetu.backend.auth.application.token.service.RevokeRefreshTokenApplicationService;
import in.bachatsetu.backend.auth.application.token.service.TokenPrincipalResolver;
import in.bachatsetu.backend.auth.application.token.service.ValidateAccessTokenApplicationService;
import in.bachatsetu.backend.auth.application.token.usecase.GenerateAccessTokenUseCase;
import in.bachatsetu.backend.auth.application.token.usecase.GenerateRefreshTokenUseCase;
import in.bachatsetu.backend.auth.application.token.usecase.RefreshAccessTokenUseCase;
import in.bachatsetu.backend.auth.application.token.usecase.RevokeRefreshTokenUseCase;
import in.bachatsetu.backend.auth.application.token.usecase.ValidateAccessTokenUseCase;
import in.bachatsetu.backend.auth.domain.port.PermissionRepository;
import in.bachatsetu.backend.auth.domain.port.RefreshTokenRepository;
import in.bachatsetu.backend.auth.domain.port.RoleRepository;
import in.bachatsetu.backend.auth.domain.port.UserRepository;
import in.bachatsetu.backend.infrastructure.auth.config.AuthenticationTokenProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Outer composition root for framework-free token application services. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "bachatsetu.authentication.token",
        name = "enabled",
        havingValue = "true")
@ConditionalOnBean({
    UserRepository.class,
    RoleRepository.class,
    PermissionRepository.class,
    RefreshTokenRepository.class,
    JwtProviderPort.class,
    TokenHasherPort.class,
    TokenClockPort.class
})
public class AuthenticationTokenApplicationConfig {

    @Bean
    TokenPrincipalResolver tokenPrincipalResolver(
            UserRepository users,
            RoleRepository roles,
            PermissionRepository permissions) {
        return new TokenPrincipalResolver(users, roles, permissions);
    }

    @Bean
    RefreshTokenCredentialVerifier refreshTokenCredentialVerifier(
            RefreshTokenRepository repository,
            TokenHasherPort hasher) {
        return new RefreshTokenCredentialVerifier(repository, hasher);
    }

    @Bean
    GenerateAccessTokenUseCase generateAccessTokenUseCase(
            TokenPrincipalResolver principals,
            JwtProviderPort jwtProvider) {
        return new GenerateAccessTokenApplicationService(principals, jwtProvider);
    }

    @Bean
    GenerateRefreshTokenUseCase generateRefreshTokenUseCase(
            TokenPrincipalResolver principals,
            RefreshTokenRepository repository,
            TokenHasherPort hasher,
            TokenClockPort clock,
            AuthenticationTokenProperties properties) {
        return new GenerateRefreshTokenApplicationService(
                principals, repository, hasher, clock, properties.refreshTokenExpiry());
    }

    @Bean
    RefreshAccessTokenUseCase refreshAccessTokenUseCase(
            RefreshTokenCredentialVerifier verifier,
            RefreshTokenRepository repository,
            TokenPrincipalResolver principals,
            JwtProviderPort jwtProvider,
            TokenHasherPort hasher,
            TokenClockPort clock,
            AuthenticationTokenProperties properties) {
        return new RefreshAccessTokenApplicationService(
                verifier,
                repository,
                principals,
                jwtProvider,
                hasher,
                clock,
                properties.refreshTokenExpiry());
    }

    @Bean
    RevokeRefreshTokenUseCase revokeRefreshTokenUseCase(
            RefreshTokenCredentialVerifier verifier,
            RefreshTokenRepository repository,
            TokenClockPort clock) {
        return new RevokeRefreshTokenApplicationService(verifier, repository, clock);
    }

    @Bean
    ValidateAccessTokenUseCase validateAccessTokenUseCase(JwtProviderPort jwtProvider) {
        return new ValidateAccessTokenApplicationService(jwtProvider);
    }
}
