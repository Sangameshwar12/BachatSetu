package in.bachatsetu.backend.auth.domain.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Six-digit one-time password with redacted string rendering.
 */
public final class OtpCode {

    private static final Pattern SIX_DIGITS = Pattern.compile("^\\d{6}$");

    private final String value;

    private OtpCode(String value) {
        this.value = value;
    }

    public static OtpCode of(String value) {
        Objects.requireNonNull(value, "OTP code must not be null");
        if (!SIX_DIGITS.matcher(value).matches()) {
            throw new IllegalArgumentException("OTP code must contain exactly six digits");
        }
        return new OtpCode(value);
    }

    public String value() {
        return value;
    }

    /**
     * Compares OTP values without content-dependent early exit.
     *
     * @param candidate candidate code
     * @return whether the codes match
     */
    public boolean matches(OtpCode candidate) {
        Objects.requireNonNull(candidate, "candidate OTP code must not be null");
        return MessageDigest.isEqual(
                value.getBytes(StandardCharsets.US_ASCII),
                candidate.value.getBytes(StandardCharsets.US_ASCII));
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof OtpCode that && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return "OtpCode[REDACTED]";
    }
}
