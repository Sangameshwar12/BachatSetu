package in.bachatsetu.backend.email.domain.model;

import java.util.Objects;

/** A validated destination or sender email address. Never logged in full — see infrastructure masking. */
public record EmailAddress(String value) {

    public EmailAddress {
        Objects.requireNonNull(value, "email address must not be null");
        String trimmed = value.trim();
        if (trimmed.isEmpty() || !trimmed.contains("@") || trimmed.startsWith("@") || trimmed.endsWith("@")) {
            throw new IllegalArgumentException("email address is not well-formed");
        }
        value = trimmed;
    }
}
