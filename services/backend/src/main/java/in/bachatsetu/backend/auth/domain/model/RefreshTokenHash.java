package in.bachatsetu.backend.auth.domain.model;

import java.util.Objects;

/** Opaque one-way hash of refresh-token credential material. */
public record RefreshTokenHash(String value) {

    private static final int MINIMUM_LENGTH = 32;
    private static final int MAXIMUM_LENGTH = 255;

    public RefreshTokenHash {
        Objects.requireNonNull(value, "refresh token hash must not be null");
        if (value.length() < MINIMUM_LENGTH || value.length() > MAXIMUM_LENGTH) {
            throw new IllegalArgumentException("refresh token hash length is invalid");
        }
    }

    public static RefreshTokenHash encoded(String value) {
        return new RefreshTokenHash(value);
    }

    @Override
    public String toString() {
        return "[REDACTED]";
    }
}
