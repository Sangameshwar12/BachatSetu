package in.bachatsetu.backend.auth.domain.model;

import java.util.Locale;
import java.util.Objects;

public record LoginIdentifier(Type type, String normalizedValue) {

    public LoginIdentifier {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(normalizedValue, "normalizedValue must not be null");
        normalizedValue = normalizedValue.strip().toLowerCase(Locale.ROOT);
        if (normalizedValue.isEmpty() || normalizedValue.length() > 254) {
            throw new IllegalArgumentException("login identifier length is invalid");
        }
    }

    public enum Type {
        EMAIL,
        PHONE_NUMBER
    }
}
