package in.bachatsetu.backend.infrastructure.auth.config;

import in.bachatsetu.backend.auth.domain.model.OtpVerification;
import in.bachatsetu.backend.auth.domain.service.OtpPolicyService;
import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Strongly typed authentication infrastructure settings. */
@ConfigurationProperties(prefix = "bachatsetu.authentication")
public record AuthenticationProperties(
        Duration otpValidity,
        int resendLimit,
        int verifyLimit,
        int hashStrength) {

    private static final int MINIMUM_HASH_STRENGTH = 10;
    private static final int MAXIMUM_HASH_STRENGTH = 16;

    public AuthenticationProperties {
        Objects.requireNonNull(otpValidity, "OTP validity must not be null");
        if (!OtpPolicyService.VALIDITY.equals(otpValidity)) {
            throw new IllegalArgumentException("OTP validity must remain five minutes");
        }
        if (resendLimit != OtpVerification.MAXIMUM_RESENDS) {
            throw new IllegalArgumentException("OTP resend limit must remain three");
        }
        if (verifyLimit != OtpVerification.MAXIMUM_VERIFICATION_ATTEMPTS) {
            throw new IllegalArgumentException("OTP verification limit must remain five");
        }
        if (hashStrength < MINIMUM_HASH_STRENGTH || hashStrength > MAXIMUM_HASH_STRENGTH) {
            throw new IllegalArgumentException("BCrypt hash strength must be between 10 and 16");
        }
    }
}
