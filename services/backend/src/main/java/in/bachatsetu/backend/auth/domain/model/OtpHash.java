package in.bachatsetu.backend.auth.domain.model;

import java.util.Objects;

/** Opaque, encoded OTP hash suitable for persistence. */
public final class OtpHash {

    private static final int MINIMUM_LENGTH = 32;
    private static final int MAXIMUM_LENGTH = 255;

    private final String value;

    private OtpHash(String value) {
        this.value = value;
    }

    public static OtpHash encoded(String value) {
        Objects.requireNonNull(value, "OTP hash must not be null");
        if (!value.equals(value.strip())
                || value.length() < MINIMUM_LENGTH
                || value.length() > MAXIMUM_LENGTH
                || value.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("OTP hash must be a supported opaque encoding");
        }
        return new OtpHash(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof OtpHash that && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return "OtpHash[REDACTED]";
    }
}
