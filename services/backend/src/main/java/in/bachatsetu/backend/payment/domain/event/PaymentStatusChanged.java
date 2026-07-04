package in.bachatsetu.backend.payment.domain.event;

import in.bachatsetu.backend.payment.domain.model.PaymentStatus;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PaymentStatusChanged(
        UUID eventId,
        AggregateId aggregateId,
        PaymentStatus previousStatus,
        PaymentStatus currentStatus,
        Instant occurredAt) implements DomainEvent {

    public PaymentStatusChanged {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(previousStatus, "previousStatus must not be null");
        Objects.requireNonNull(currentStatus, "currentStatus must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
