package in.bachatsetu.backend.configuration.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
import in.bachatsetu.backend.auth.domain.port.PermissionRepository;
import in.bachatsetu.backend.auth.domain.port.RefreshTokenRepository;
import in.bachatsetu.backend.auth.domain.port.RoleRepository;
import in.bachatsetu.backend.auth.domain.port.UserRepository;
import in.bachatsetu.backend.infrastructure.auth.config.AuthenticationTokenProperties;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class AuthenticationTokenApplicationConfigTest {

    @Test
    void composesAllTokenUseCases() {
        AuthenticationTokenApplicationConfig config = new AuthenticationTokenApplicationConfig();
        UserRepository users = mock(UserRepository.class);
        RoleRepository roles = mock(RoleRepository.class);
        PermissionRepository permissions = mock(PermissionRepository.class);
        RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
        JwtProviderPort jwt = mock(JwtProviderPort.class);
        TokenHasherPort hasher = mock(TokenHasherPort.class);
        TokenClockPort clock = mock(TokenClockPort.class);
        AuthenticationTokenProperties properties = properties();
        TokenPrincipalResolver principals = config.tokenPrincipalResolver(users, roles, permissions);
        RefreshTokenCredentialVerifier verifier = config.refreshTokenCredentialVerifier(refreshTokens, hasher);

        assertThat(config.generateAccessTokenUseCase(principals, jwt))
                .isInstanceOf(GenerateAccessTokenApplicationService.class);
        assertThat(config.generateRefreshTokenUseCase(principals, refreshTokens, hasher, clock, properties))
                .isInstanceOf(GenerateRefreshTokenApplicationService.class);
        assertThat(config.refreshAccessTokenUseCase(
                        verifier, refreshTokens, principals, jwt, hasher, clock, properties))
                .isInstanceOf(RefreshAccessTokenApplicationService.class);
        assertThat(config.revokeRefreshTokenUseCase(verifier, refreshTokens, clock))
                .isInstanceOf(RevokeRefreshTokenApplicationService.class);
        assertThat(config.validateAccessTokenUseCase(jwt))
                .isInstanceOf(ValidateAccessTokenApplicationService.class);
    }

    private AuthenticationTokenProperties properties() {
        return new AuthenticationTokenProperties(
                true,
                Duration.ofMinutes(15),
                Duration.ofDays(30),
                "bachatsetu",
                "bachatsetu-api",
                Base64.getEncoder().encodeToString("K".repeat(64).getBytes()),
                Duration.ofSeconds(30),
                12,
                1);
    }
}
