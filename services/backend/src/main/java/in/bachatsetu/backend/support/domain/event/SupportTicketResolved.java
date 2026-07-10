package in.bachatsetu.backend.support.domain.event;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SupportTicketResolved(
        UUID eventId,
        AggregateId aggregateId,
        Instant occurredAt) implements DomainEvent {

    public SupportTicketResolved {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
