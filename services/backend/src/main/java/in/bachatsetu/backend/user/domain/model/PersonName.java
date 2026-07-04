package in.bachatsetu.backend.user.domain.model;

import java.util.Objects;

public record PersonName(String givenName, String familyName) {

    public PersonName {
        givenName = normalizeRequired(givenName, "givenName");
        familyName = normalizeOptional(familyName);
    }

    public String displayName() {
        return familyName == null ? givenName : givenName + " " + familyName;
    }

    private static String normalizeRequired(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        String normalized = value.strip();
        if (normalized.isEmpty() || normalized.length() > 100) {
            throw new IllegalArgumentException(field + " length is invalid");
        }
        return normalized;
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() > 100) {
            throw new IllegalArgumentException("familyName is too long");
        }
        return normalized;
    }
}
