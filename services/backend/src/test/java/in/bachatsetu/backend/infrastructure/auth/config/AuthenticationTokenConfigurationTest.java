package in.bachatsetu.backend.infrastructure.auth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import in.bachatsetu.backend.auth.application.token.port.JwtProviderPort;
import in.bachatsetu.backend.auth.application.token.port.TokenClockPort;
import in.bachatsetu.backend.auth.application.token.port.TokenHasherPort;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AuthenticationTokenConfigurationTest {

    private static final String SECRET = Base64.getEncoder().encodeToString("K".repeat(64).getBytes());

    @Test
    void bindsEnabledTokenInfrastructure() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        AuthenticationInfrastructureConfig.class,
                        AuthenticationTokenInfrastructureConfig.class))
                .withPropertyValues(
                        "bachatsetu.cache.enabled=false",
                        "bachatsetu.authentication.otp-validity=5m",
                        "bachatsetu.authentication.resend-limit=3",
                        "bachatsetu.authentication.verify-limit=5",
                        "bachatsetu.authentication.hash-strength=12",
                        "bachatsetu.authentication.token.enabled=true",
                        "bachatsetu.authentication.token.access-token-expiry=15m",
                        "bachatsetu.authentication.token.refresh-token-expiry=30d",
                        "bachatsetu.authentication.token.issuer=bachatsetu",
                        "bachatsetu.authentication.token.audience=bachatsetu-api",
                        "bachatsetu.authentication.token.signing-secret=" + SECRET,
                        "bachatsetu.authentication.token.clock-skew=30s",
                        "bachatsetu.authentication.token.hash-strength=12",
                        "bachatsetu.authentication.token.jwt-version=1")
                .run(context -> {
                    assertThat(context).hasSingleBean(TokenClockPort.class);
                    assertThat(context).hasSingleBean(TokenHasherPort.class);
                    assertThat(context).hasSingleBean(JwtProviderPort.class);
                    assertThat(context).hasSingleBean(AuthenticationTokenProperties.class);
                });
    }

    @Test
    void leavesTokenInfrastructureDisabledByDefault() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(AuthenticationTokenInfrastructureConfig.class))
                .run(context -> assertThat(context).doesNotHaveBean(JwtProviderPort.class));
    }

    @Test
    void createsAdaptersThroughConfigurationMethods() {
        AuthenticationTokenProperties properties = validProperties();
        AuthenticationTokenInfrastructureConfig config = new AuthenticationTokenInfrastructureConfig();
        Clock clock = Clock.systemUTC();
        SecureRandom random = new SecureRandom();
        TokenClockPort tokenClock = config.tokenClockPort(clock);
        var encoder = config.refreshTokenPasswordEncoder(properties, random);

        assertThat(config.tokenHasherPort(random, encoder)).isInstanceOf(TokenHasherPort.class);
        assertThat(config.jwtProviderPort(properties, tokenClock)).isInstanceOf(JwtProviderPort.class);
    }

    @Test
    void validatesEverySecurityPropertyBoundaryAndRedactsSecret() {
        AuthenticationTokenProperties valid = validProperties();
        assertThat(valid.toString()).doesNotContain(SECRET);
        assertThat(valid.enabled()).isTrue();

        assertThatIllegalArgumentException().isThrownBy(() -> properties(
                Duration.ZERO, Duration.ofDays(30), "issuer", "audience", SECRET, Duration.ZERO, 12, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> properties(
                Duration.ofMinutes(15), Duration.ofMinutes(15), "issuer", "audience", SECRET, Duration.ZERO, 12, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> properties(
                Duration.ofMinutes(15), Duration.ofDays(30), " ", "audience", SECRET, Duration.ZERO, 12, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> properties(
                Duration.ofMinutes(15), Duration.ofDays(30), "issuer", "audience",
                Base64.getEncoder().encodeToString("short".getBytes()), Duration.ZERO, 12, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> properties(
                Duration.ofMinutes(15), Duration.ofDays(30), "issuer", "audience", SECRET, Duration.ofSeconds(-1), 12, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> properties(
                Duration.ofMinutes(15), Duration.ofDays(30), "issuer", "audience", SECRET, Duration.ofMinutes(3), 12, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> properties(
                Duration.ofMinutes(15), Duration.ofDays(30), "issuer", "audience", SECRET, Duration.ZERO, 9, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> properties(
                Duration.ofMinutes(15), Duration.ofDays(30), "issuer", "audience", SECRET, Duration.ZERO, 12, 0));
    }

    private AuthenticationTokenProperties validProperties() {
        return properties(
                Duration.ofMinutes(15),
                Duration.ofDays(30),
                "bachatsetu",
                "bachatsetu-api",
                SECRET,
                Duration.ofSeconds(30),
                12,
                1);
    }

    private AuthenticationTokenProperties properties(
            Duration accessExpiry,
            Duration refreshExpiry,
            String issuer,
            String audience,
            String secret,
            Duration skew,
            int hashStrength,
            int version) {
        return new AuthenticationTokenProperties(
                true,
                accessExpiry,
                refreshExpiry,
                issuer,
                audience,
                secret,
                skew,
                hashStrength,
                version);
    }
}
