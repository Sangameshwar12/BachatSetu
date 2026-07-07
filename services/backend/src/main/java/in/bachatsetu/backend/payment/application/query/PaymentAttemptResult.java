package in.bachatsetu.backend.payment.application.query;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Safe application view of one payment attempt. */
public record PaymentAttemptResult(
        UUID attemptId,
        int sequence,
        Instant initiatedAt,
        String status,
        String provider,
        String transactionId,
        String failureCode) {

    public PaymentAttemptResult {
        Objects.requireNonNull(attemptId, "attempt id must not be null");
        Objects.requireNonNull(initiatedAt, "initiated at must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}
