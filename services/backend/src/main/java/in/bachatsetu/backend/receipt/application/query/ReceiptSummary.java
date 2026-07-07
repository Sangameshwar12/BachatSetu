package in.bachatsetu.backend.receipt.application.query;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Compact receipt view optimized for list use cases. */
public record ReceiptSummary(
        UUID receiptId,
        String number,
        long totalAmountPaise,
        String currencyCode,
        String status,
        Instant generatedAt) {

    public ReceiptSummary {
        Objects.requireNonNull(receiptId, "receipt id must not be null");
        Objects.requireNonNull(number, "number must not be null");
        Objects.requireNonNull(currencyCode, "currency code must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(generatedAt, "generated at must not be null");
    }
}
