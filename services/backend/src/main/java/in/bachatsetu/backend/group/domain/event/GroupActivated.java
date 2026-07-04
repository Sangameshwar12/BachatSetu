package in.bachatsetu.backend.group.domain.event;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record GroupActivated(
        UUID eventId,
        AggregateId aggregateId,
        int activeMemberCount,
        Instant occurredAt) implements DomainEvent {

    public GroupActivated {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        if (activeMemberCount < 1) {
            throw new IllegalArgumentException("activeMemberCount must be positive");
        }
    }
}
