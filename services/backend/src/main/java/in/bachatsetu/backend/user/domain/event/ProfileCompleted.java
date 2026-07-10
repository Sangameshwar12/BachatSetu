package in.bachatsetu.backend.user.domain.event;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Emitted once a newly registered user finishes the post-signup onboarding step. */
public record ProfileCompleted(
        UUID eventId,
        AggregateId aggregateId,
        Instant occurredAt) implements DomainEvent {

    public ProfileCompleted {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
