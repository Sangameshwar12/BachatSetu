package in.bachatsetu.backend.configuration.auth;

import in.bachatsetu.backend.auth.application.port.DomainEventPublisherPort;
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
import in.bachatsetu.backend.infrastructure.persistence.adapter.ConditionalOnPersistenceRepositories;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Outer composition root for framework-free token application services.
 *
 * <p>Gated on {@code bachatsetu.persistence.repositories.enabled} rather than a
 * cross-configuration-class {@code @ConditionalOnBean} check for its required ports: regular
 * (non-auto-configuration) {@code @Configuration} classes discovered by component scanning have
 * no guaranteed processing order relative to one another, so a class-level
 * {@code @ConditionalOnBean} referencing ports defined by sibling configuration classes was
 * evaluated non-deterministically and could skip this configuration even when every required
 * port was actually present.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "bachatsetu.authentication.token",
        name = "enabled",
        havingValue = "true")
@ConditionalOnPersistenceRepositories
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
            AuthenticationTokenProperties properties,
            DomainEventPublisherPort eventPublisher) {
        return new RefreshAccessTokenApplicationService(
                verifier,
                repository,
                principals,
                jwtProvider,
                hasher,
                clock,
                properties.refreshTokenExpiry(),
                eventPublisher);
    }

    @Bean
    RevokeRefreshTokenUseCase revokeRefreshTokenUseCase(
            RefreshTokenCredentialVerifier verifier,
            RefreshTokenRepository repository,
            TokenClockPort clock,
            DomainEventPublisherPort eventPublisher) {
        return new RevokeRefreshTokenApplicationService(verifier, repository, clock, eventPublisher);
    }

    @Bean
    ValidateAccessTokenUseCase validateAccessTokenUseCase(JwtProviderPort jwtProvider) {
        return new ValidateAccessTokenApplicationService(jwtProvider);
    }
}
