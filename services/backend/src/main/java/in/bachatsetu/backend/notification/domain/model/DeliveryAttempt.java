package in.bachatsetu.backend.notification.domain.model;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;

public final class DeliveryAttempt {

    private final AggregateId id;
    private final int sequence;
    private final Instant startedAt;
    private DeliveryAttemptStatus status;
    private String providerMessageId;
    private String failureCode;

    public DeliveryAttempt(
            AggregateId id,
            int sequence,
            Instant startedAt,
            DeliveryAttemptStatus status,
            String providerMessageId,
            String failureCode) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        if (sequence < 1) {
            throw new IllegalArgumentException("sequence must be positive");
        }
        this.sequence = sequence;
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.providerMessageId = normalizeOptional(providerMessageId, 160);
        this.failureCode = normalizeOptional(failureCode, 80);
    }

    public void accept(String messageId) {
        providerMessageId = normalizeRequired(messageId, "messageId", 160);
        failureCode = null;
        status = DeliveryAttemptStatus.ACCEPTED;
    }

    public void deliver() {
        if (status != DeliveryAttemptStatus.ACCEPTED) {
            throw new IllegalStateException("only an accepted attempt can be delivered");
        }
        status = DeliveryAttemptStatus.DELIVERED;
    }

    public void fail(String code) {
        if (status == DeliveryAttemptStatus.DELIVERED) {
            throw new IllegalStateException("delivered attempt cannot fail");
        }
        failureCode = normalizeRequired(code, "code", 80);
        status = DeliveryAttemptStatus.FAILED;
    }

    private static String normalizeRequired(String value, String field, int maximumLength) {
        Objects.requireNonNull(value, field + " must not be null");
        String normalized = value.strip();
        if (normalized.isEmpty() || normalized.length() > maximumLength) {
            throw new IllegalArgumentException(field + " length is invalid");
        }
        return normalized;
    }

    private static String normalizeOptional(String value, int maximumLength) {
        return value == null ? null : normalizeRequired(value, "value", maximumLength);
    }

    public AggregateId id() { return id; }
    public int sequence() { return sequence; }
    public Instant startedAt() { return startedAt; }
    public DeliveryAttemptStatus status() { return status; }
    public String providerMessageId() { return providerMessageId; }
    public String failureCode() { return failureCode; }
}
