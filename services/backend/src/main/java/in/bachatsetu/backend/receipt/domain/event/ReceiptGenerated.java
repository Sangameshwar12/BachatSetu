package in.bachatsetu.backend.receipt.domain.event;

import in.bachatsetu.backend.receipt.domain.model.ReceiptNumber;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import in.bachatsetu.backend.shared.domain.Money;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ReceiptGenerated(
        UUID eventId,
        AggregateId aggregateId,
        AggregateId paymentId,
        ReceiptNumber receiptNumber,
        Money total,
        Instant occurredAt) implements DomainEvent {

    public ReceiptGenerated {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(paymentId, "paymentId must not be null");
        Objects.requireNonNull(receiptNumber, "receiptNumber must not be null");
        Objects.requireNonNull(total, "total must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
