package in.bachatsetu.backend.auth.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Encoded password hash. Raw passwords cannot be represented by this type.
 */
public final class PasswordHash {

    private static final Pattern BCRYPT = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");
    private static final Pattern ARGON2 = Pattern.compile(
            "^\\$argon2(?:id|i|d)\\$v=\\d+\\$m=\\d+,t=\\d+,p=\\d+\\$[A-Za-z0-9+/]+={0,2}\\$[A-Za-z0-9+/]+={0,2}$");

    private final String value;

    private PasswordHash(String value) {
        this.value = value;
    }

    /**
     * Creates a password hash from a recognized encoded representation.
     *
     * @param value bcrypt or Argon2 encoded hash
     * @return validated password hash
     */
    public static PasswordHash encoded(String value) {
        Objects.requireNonNull(value, "password hash must not be null");
        if (!value.equals(value.strip()) || !(BCRYPT.matcher(value).matches() || ARGON2.matcher(value).matches())) {
            throw new IllegalArgumentException("password hash must use a supported encoded format");
        }
        return new PasswordHash(value);
    }

    /**
     * Returns the encoded hash for a password-verification adapter.
     *
     * @return encoded value
     */
    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof PasswordHash that && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return "PasswordHash[REDACTED]";
    }
}
