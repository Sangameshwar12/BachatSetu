package in.bachatsetu.backend.group.domain.event;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Signals that a member joined an active savings group. */
public record MemberJoined(
        UUID eventId,
        AggregateId aggregateId,
        AggregateId memberId,
        Instant occurredAt) implements DomainEvent {

    public MemberJoined {
        Objects.requireNonNull(eventId, "event id must not be null");
        Objects.requireNonNull(aggregateId, "aggregate id must not be null");
        Objects.requireNonNull(memberId, "member id must not be null");
        Objects.requireNonNull(occurredAt, "occurred at must not be null");
    }
}
