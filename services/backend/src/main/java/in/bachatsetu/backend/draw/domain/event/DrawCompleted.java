package in.bachatsetu.backend.draw.domain.event;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record DrawCompleted(
        UUID eventId,
        AggregateId aggregateId,
        AggregateId winnerMemberId,
        Instant occurredAt) implements DomainEvent {

    public DrawCompleted {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(winnerMemberId, "winnerMemberId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
