package in.bachatsetu.backend.payment.application.query;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Complete application view of a Payment aggregate. */
public record PaymentResult(
        UUID paymentId,
        UUID tenantId,
        UUID groupId,
        UUID memberId,
        String reference,
        long amountPaise,
        String currencyCode,
        String method,
        String status,
        String reconciliationStatus,
        List<PaymentAttemptResult> attempts,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    public PaymentResult {
        Objects.requireNonNull(paymentId, "payment id must not be null");
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(groupId, "group id must not be null");
        Objects.requireNonNull(memberId, "member id must not be null");
        Objects.requireNonNull(reference, "reference must not be null");
        Objects.requireNonNull(currencyCode, "currency code must not be null");
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(reconciliationStatus, "reconciliation status must not be null");
        attempts = List.copyOf(Objects.requireNonNull(attempts, "attempts must not be null"));
        Objects.requireNonNull(createdAt, "created at must not be null");
        Objects.requireNonNull(updatedAt, "updated at must not be null");
    }
}
