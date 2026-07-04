package in.bachatsetu.backend.receipt.domain.event;

import in.bachatsetu.backend.receipt.domain.model.ReceiptNumber;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ReceiptCancelled(
        UUID eventId,
        AggregateId aggregateId,
        ReceiptNumber receiptNumber,
        String reason,
        Instant occurredAt) implements DomainEvent {

    public ReceiptCancelled {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(receiptNumber, "receiptNumber must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
