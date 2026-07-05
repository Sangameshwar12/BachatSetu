package in.bachatsetu.backend.infrastructure.auth.config;

import io.jsonwebtoken.io.Decoders;
import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Strongly typed JWT and refresh-token security settings. */
@ConfigurationProperties(prefix = "bachatsetu.authentication.token")
public record AuthenticationTokenProperties(
        boolean enabled,
        Duration accessTokenExpiry,
        Duration refreshTokenExpiry,
        String issuer,
        String audience,
        String signingSecret,
        Duration clockSkew,
        int hashStrength,
        int jwtVersion) {

    private static final int HS512_KEY_BYTES = 64;

    public AuthenticationTokenProperties {
        requirePositive(accessTokenExpiry, "access token expiry");
        requirePositive(refreshTokenExpiry, "refresh token expiry");
        if (refreshTokenExpiry.compareTo(accessTokenExpiry) <= 0) {
            throw new IllegalArgumentException("refresh token expiry must exceed access token expiry");
        }
        issuer = requireText(issuer, "JWT issuer");
        audience = requireText(audience, "JWT audience");
        Objects.requireNonNull(signingSecret, "JWT signing secret must not be null");
        if (Decoders.BASE64.decode(signingSecret).length < HS512_KEY_BYTES) {
            throw new IllegalArgumentException("HS512 signing secret must contain at least 512 bits");
        }
        Objects.requireNonNull(clockSkew, "JWT clock skew must not be null");
        if (clockSkew.isNegative() || clockSkew.compareTo(Duration.ofMinutes(2)) > 0) {
            throw new IllegalArgumentException("JWT clock skew must be between zero and two minutes");
        }
        if (hashStrength < 10 || hashStrength > 16) {
            throw new IllegalArgumentException("refresh token BCrypt strength must be between 10 and 16");
        }
        if (jwtVersion < 1) {
            throw new IllegalArgumentException("JWT version must be positive");
        }
    }

    private static void requirePositive(Duration value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isNegative() || value.isZero()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    @Override
    public String toString() {
        return "AuthenticationTokenProperties[signingSecret=[REDACTED]]";
    }
}
