package in.bachatsetu.backend.receipt.application.query;

import java.util.Objects;
import java.util.UUID;

/** Safe application view of one receipt line. */
public record ReceiptLineResult(
        UUID lineId,
        String type,
        String description,
        long amountPaise,
        String currencyCode) {

    public ReceiptLineResult {
        Objects.requireNonNull(lineId, "line id must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(currencyCode, "currency code must not be null");
    }
}
