package in.bachatsetu.backend.payment.domain.event;

import in.bachatsetu.backend.payment.domain.model.PaymentReference;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import in.bachatsetu.backend.shared.domain.Money;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PaymentInitiated(
        UUID eventId,
        AggregateId aggregateId,
        AggregateId memberId,
        PaymentReference paymentReference,
        Money amount,
        Instant occurredAt) implements DomainEvent {

    public PaymentInitiated {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(memberId, "memberId must not be null");
        Objects.requireNonNull(paymentReference, "paymentReference must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
