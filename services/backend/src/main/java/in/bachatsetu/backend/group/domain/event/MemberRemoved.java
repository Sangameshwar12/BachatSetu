package in.bachatsetu.backend.group.domain.event;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Signals that a non-owner member left a savings group. */
public record MemberRemoved(
        UUID eventId,
        AggregateId aggregateId,
        AggregateId memberId,
        Instant occurredAt) implements DomainEvent {

    public MemberRemoved {
        Objects.requireNonNull(eventId, "event id must not be null");
        Objects.requireNonNull(aggregateId, "aggregate id must not be null");
        Objects.requireNonNull(memberId, "member id must not be null");
        Objects.requireNonNull(occurredAt, "occurred at must not be null");
    }
}
