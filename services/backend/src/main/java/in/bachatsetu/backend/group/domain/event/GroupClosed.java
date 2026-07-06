package in.bachatsetu.backend.group.domain.event;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Signals that a savings group entered its terminal closed state. */
public record GroupClosed(
        UUID eventId,
        AggregateId aggregateId,
        Instant occurredAt) implements DomainEvent {

    public GroupClosed {
        Objects.requireNonNull(eventId, "event id must not be null");
        Objects.requireNonNull(aggregateId, "aggregate id must not be null");
        Objects.requireNonNull(occurredAt, "occurred at must not be null");
    }
}
