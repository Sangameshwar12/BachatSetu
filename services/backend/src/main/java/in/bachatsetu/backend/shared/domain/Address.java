package in.bachatsetu.backend.shared.domain;

import java.util.Locale;
import java.util.Objects;

public record Address(
        String line1,
        String line2,
        String locality,
        String city,
        String state,
        String postalCode,
        String countryCode) {

    public Address {
        line1 = required(line1, "line1", 120);
        line2 = optional(line2, 120);
        locality = optional(locality, 100);
        city = required(city, "city", 100);
        state = required(state, "state", 100);
        postalCode = required(postalCode, "postalCode", 20);
        countryCode = required(countryCode, "countryCode", 2).toUpperCase(Locale.ROOT);
    }

    private static String required(String value, String field, int maximumLength) {
        Objects.requireNonNull(value, field + " must not be null");
        String normalized = value.strip();
        if (normalized.isEmpty() || normalized.length() > maximumLength) {
            throw new IllegalArgumentException(field + " length is invalid");
        }
        return normalized;
    }

    private static String optional(String value, int maximumLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() > maximumLength) {
            throw new IllegalArgumentException("optional address field is too long");
        }
        return normalized.isEmpty() ? null : normalized;
    }
}
