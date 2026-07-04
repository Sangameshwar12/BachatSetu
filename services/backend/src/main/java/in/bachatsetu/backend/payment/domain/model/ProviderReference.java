package in.bachatsetu.backend.payment.domain.model;

import java.util.Objects;

public record ProviderReference(String provider, String transactionId) {

    public ProviderReference {
        provider = required(provider, "provider", 50);
        transactionId = required(transactionId, "transactionId", 120);
    }

    private static String required(String value, String field, int maximumLength) {
        Objects.requireNonNull(value, field + " must not be null");
        String normalized = value.strip();
        if (normalized.isEmpty() || normalized.length() > maximumLength) {
            throw new IllegalArgumentException(field + " length is invalid");
        }
        return normalized;
    }
}
