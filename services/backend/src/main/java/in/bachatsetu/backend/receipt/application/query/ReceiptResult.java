package in.bachatsetu.backend.receipt.application.query;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Complete application view of a Receipt aggregate. */
public record ReceiptResult(
        UUID receiptId,
        UUID tenantId,
        UUID paymentId,
        UUID memberId,
        String number,
        List<ReceiptLineResult> lines,
        long totalAmountPaise,
        String currencyCode,
        String status,
        String cancellationReason,
        Instant generatedAt,
        Instant updatedAt,
        long version) {

    public ReceiptResult {
        Objects.requireNonNull(receiptId, "receipt id must not be null");
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(paymentId, "payment id must not be null");
        Objects.requireNonNull(memberId, "member id must not be null");
        Objects.requireNonNull(number, "number must not be null");
        lines = List.copyOf(Objects.requireNonNull(lines, "lines must not be null"));
        Objects.requireNonNull(currencyCode, "currency code must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(generatedAt, "generated at must not be null");
        Objects.requireNonNull(updatedAt, "updated at must not be null");
    }
}
