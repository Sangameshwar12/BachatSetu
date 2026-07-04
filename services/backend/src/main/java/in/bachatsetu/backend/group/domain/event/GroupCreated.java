package in.bachatsetu.backend.group.domain.event;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record GroupCreated(
        UUID eventId,
        AggregateId aggregateId,
        AggregateId tenantId,
        AggregateId organizerId,
        Instant occurredAt) implements DomainEvent {

    public GroupCreated {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(organizerId, "organizerId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
