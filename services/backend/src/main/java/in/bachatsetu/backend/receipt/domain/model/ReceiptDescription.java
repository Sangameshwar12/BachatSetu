package in.bachatsetu.backend.receipt.domain.model;

import java.util.Objects;

public record ReceiptDescription(String value) {

    public ReceiptDescription {
        Objects.requireNonNull(value, "receipt description must not be null");
        value = value.strip();
        if (value.isEmpty() || value.length() > 200) {
            throw new IllegalArgumentException("receipt description length is invalid");
        }
    }
}
