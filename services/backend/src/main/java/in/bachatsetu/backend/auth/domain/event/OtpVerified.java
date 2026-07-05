package in.bachatsetu.backend.auth.domain.event;

import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Emitted after a valid, unexpired OTP is verified. */
public record OtpVerified(
        UUID eventId,
        AggregateId aggregateId,
        UserId userId,
        Instant occurredAt) implements DomainEvent {

    public OtpVerified {
        Objects.requireNonNull(eventId, "event id must not be null");
        Objects.requireNonNull(aggregateId, "aggregate id must not be null");
        Objects.requireNonNull(userId, "user id must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
