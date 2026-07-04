package in.bachatsetu.backend.payment.domain.model;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;

public final class PaymentAttempt {

    private final AggregateId id;
    private final int sequence;
    private final Instant initiatedAt;
    private PaymentAttemptStatus status;
    private ProviderReference providerReference;
    private String failureCode;

    public PaymentAttempt(
            AggregateId id,
            int sequence,
            Instant initiatedAt,
            PaymentAttemptStatus status,
            ProviderReference providerReference,
            String failureCode) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        if (sequence < 1) {
            throw new IllegalArgumentException("sequence must be positive");
        }
        this.sequence = sequence;
        this.initiatedAt = Objects.requireNonNull(initiatedAt, "initiatedAt must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.providerReference = providerReference;
        this.failureCode = normalizeFailureCode(failureCode);
    }

    public void markSucceeded(ProviderReference reference) {
        if (status == PaymentAttemptStatus.SUCCEEDED) {
            return;
        }
        if (status == PaymentAttemptStatus.FAILED) {
            throw new IllegalStateException("failed payment attempt cannot succeed");
        }
        providerReference = Objects.requireNonNull(reference, "reference must not be null");
        failureCode = null;
        status = PaymentAttemptStatus.SUCCEEDED;
    }

    public void markFailed(String code) {
        if (status == PaymentAttemptStatus.SUCCEEDED) {
            throw new IllegalStateException("successful payment attempt cannot fail");
        }
        failureCode = normalizeFailureCode(Objects.requireNonNull(code, "code must not be null"));
        status = PaymentAttemptStatus.FAILED;
    }

    private static String normalizeFailureCode(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.isEmpty() || normalized.length() > 80) {
            throw new IllegalArgumentException("failure code length is invalid");
        }
        return normalized;
    }

    public AggregateId id() {
        return id;
    }

    public int sequence() {
        return sequence;
    }

    public Instant initiatedAt() {
        return initiatedAt;
    }

    public PaymentAttemptStatus status() {
        return status;
    }

    public ProviderReference providerReference() {
        return providerReference;
    }

    public String failureCode() {
        return failureCode;
    }
}
