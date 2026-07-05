package in.bachatsetu.backend.auth.application.token.port;

import java.util.Objects;

/** Sensitive compact JWT value that redacts string rendering. */
public final class AccessTokenValue {

    private final String value;

    private AccessTokenValue(String value) {
        this.value = Objects.requireNonNull(value, "access token must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("access token must not be blank");
        }
    }

    public static AccessTokenValue of(String value) {
        return new AccessTokenValue(value);
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return "[REDACTED]";
    }
}
