package in.bachatsetu.backend.receipt.domain.model;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.util.Objects;

public record ReceiptLine(
        AggregateId id,
        ReceiptType type,
        ReceiptDescription description,
        Money amount) {

    public ReceiptLine {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("receipt line amount must be positive");
        }
    }
}
