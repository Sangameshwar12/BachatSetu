package in.bachatsetu.backend.auth.domain.event;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AuthAccountCreated(
        UUID eventId,
        AggregateId aggregateId,
        AggregateId userId,
        Instant occurredAt) implements DomainEvent {

    public AuthAccountCreated {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
