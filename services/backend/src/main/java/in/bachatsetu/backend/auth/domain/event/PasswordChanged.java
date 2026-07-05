package in.bachatsetu.backend.auth.domain.event;

import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Emitted after a user's encoded password hash changes. */
public record PasswordChanged(UUID eventId, UserId userId, Instant occurredAt) implements DomainEvent {

    public PasswordChanged {
        Objects.requireNonNull(eventId, "event id must not be null");
        Objects.requireNonNull(userId, "user id must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    @Override
    public AggregateId aggregateId() {
        return userId.toAggregateId();
    }
}
