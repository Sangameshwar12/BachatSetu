package in.bachatsetu.backend.payment.domain.model;

import java.util.Objects;

public record IdempotencyKey(String value) {

    public IdempotencyKey {
        Objects.requireNonNull(value, "idempotency key must not be null");
        value = value.strip();
        if (value.length() < 16 || value.length() > 128) {
            throw new IllegalArgumentException("idempotency key must contain 16 to 128 characters");
        }
    }
}
