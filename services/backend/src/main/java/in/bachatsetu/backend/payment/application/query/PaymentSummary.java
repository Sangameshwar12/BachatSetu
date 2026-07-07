package in.bachatsetu.backend.payment.application.query;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Compact payment view optimized for list use cases. */
public record PaymentSummary(
        UUID paymentId,
        String reference,
        long amountPaise,
        String currencyCode,
        String method,
        String status,
        Instant createdAt) {

    public PaymentSummary {
        Objects.requireNonNull(paymentId, "payment id must not be null");
        Objects.requireNonNull(reference, "reference must not be null");
        Objects.requireNonNull(currencyCode, "currency code must not be null");
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "created at must not be null");
    }
}
